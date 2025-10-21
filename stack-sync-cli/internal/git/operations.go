package git

import (
	"fmt"
	"os"
	"path/filepath"
	"strings"
	"time"

	"github.com/go-git/go-git/v5"
	"github.com/go-git/go-git/v5/plumbing"
	"github.com/go-git/go-git/v5/plumbing/object"
	"github.com/go-git/go-git/v5/plumbing/transport"
	"github.com/go-git/go-git/v5/plumbing/transport/http"
	"github.com/go-git/go-git/v5/plumbing/transport/ssh"
	"github.com/stackfilesync/stack-sync-cli/pkg/models"
)

// Operations provides Git operations wrapper
type Operations struct {
	repo *git.Repository
	path string
}

// New creates a new Git operations instance
func New(repoPath string) (*Operations, error) {
	repo, err := git.PlainOpen(repoPath)
	if err != nil {
		return nil, err
	}

	return &Operations{
		repo: repo,
		path: repoPath,
	}, nil
}

// Clone clones a repository to the specified path with authentication
func Clone(repo *models.Repository, path string) (*Operations, error) {
	// Check if path exists and clean it (safe for temp directories)
	if _, err := os.Stat(path); !os.IsNotExist(err) {
		// Remove existing directory (likely leftover temp directory)
		if err := os.RemoveAll(path); err != nil {
			return nil, fmt.Errorf("failed to clean existing path: %w", err)
		}
	}

	// Create parent directory
	if err := os.MkdirAll(filepath.Dir(path), 0755); err != nil {
		return nil, fmt.Errorf("failed to create directory: %w", err)
	}

	// Determine authentication based on repo configuration
	auth, err := getAuth(repo)
	if err != nil {
		return nil, fmt.Errorf("failed to setup authentication: %w", err)
	}

	// Clone the repository
	gitRepo, err := git.PlainClone(path, false, &git.CloneOptions{
		URL:      repo.URL,
		Auth:     auth,
		Progress: os.Stdout,
	})
	if err != nil {
		return nil, fmt.Errorf("failed to clone repository: %w", err)
	}

	return &Operations{
		repo: gitRepo,
		path: path,
	}, nil
}

// getAuth returns appropriate authentication based on repository configuration
func getAuth(repo *models.Repository) (transport.AuthMethod, error) {
	// Determine auth type by URL or explicit repo_type
	isSSH := strings.HasPrefix(repo.URL, "git@") ||
	         strings.HasPrefix(repo.URL, "ssh://") ||
	         strings.EqualFold(repo.RepoType, "SSH")

	if isSSH {
		// SSH authentication
		return getSSHAuth()
	}

	// HTTPS authentication
	if repo.Username != "" || repo.Password != "" {
		// Use basic auth with username/password or token
		username := repo.Username
		password := repo.Password

		// If username is empty but password is provided, treat password as token
		if username == "" && password != "" {
			username = "git" // Some Git servers accept any username with token
		}

		if username != "" && password != "" {
			return &http.BasicAuth{
				Username: username,
				Password: password,
			}, nil
		}
	}

	// No authentication (for public repos)
	return nil, nil
}

// Pull pulls the latest changes
func (o *Operations) Pull() error {
	w, err := o.repo.Worktree()
	if err != nil {
		return err
	}

	auth, _ := getSSHAuth()

	err = w.Pull(&git.PullOptions{
		RemoteName: "origin",
		Auth:       auth,
		Progress:   os.Stdout,
	})

	if err != nil && err != git.NoErrAlreadyUpToDate {
		return err
	}

	return nil
}

// Status returns the repository status
func (o *Operations) Status() (git.Status, error) {
	w, err := o.repo.Worktree()
	if err != nil {
		return nil, err
	}

	return w.Status()
}

// GetStatus returns the sync status of the repository
func (o *Operations) GetStatus() (models.SyncStatus, error) {
	status, err := o.Status()
	if err != nil {
		return models.StatusError, err
	}

	// Check if there are any changes
	if !status.IsClean() {
		return models.StatusModified, nil
	}

	// Check if behind remote
	isBehind, err := o.IsBehindRemote()
	if err != nil {
		return models.StatusError, err
	}

	if isBehind {
		return models.StatusModified, nil
	}

	return models.StatusUpToDate, nil
}

// IsBehindRemote checks if local is behind remote
func (o *Operations) IsBehindRemote() (bool, error) {
	head, err := o.repo.Head()
	if err != nil {
		return false, err
	}

	remote, err := o.repo.Remote("origin")
	if err != nil {
		return false, err
	}

	refs, err := remote.List(&git.ListOptions{})
	if err != nil {
		return false, err
	}

	var remoteHash plumbing.Hash
	for _, ref := range refs {
		if ref.Name() == head.Name() {
			remoteHash = ref.Hash()
			break
		}
	}

	return head.Hash() != remoteHash, nil
}

// GetModifiedFiles returns a list of modified files
func (o *Operations) GetModifiedFiles() ([]string, error) {
	status, err := o.Status()
	if err != nil {
		return nil, err
	}

	var files []string
	for file, _ := range status {
		files = append(files, file)
	}

	return files, nil
}

// GetLastCommit returns the last commit info
func (o *Operations) GetLastCommit() (*object.Commit, error) {
	head, err := o.repo.Head()
	if err != nil {
		return nil, err
	}

	return o.repo.CommitObject(head.Hash())
}

// Add stages files matching the patterns
func (o *Operations) Add(patterns []string) error {
	w, err := o.repo.Worktree()
	if err != nil {
		return err
	}

	for _, pattern := range patterns {
		if err := w.AddGlob(pattern); err != nil {
			return err
		}
	}

	return nil
}

// Commit creates a new commit
func (o *Operations) Commit(message string) error {
	w, err := o.repo.Worktree()
	if err != nil {
		return err
	}

	_, err = w.Commit(message, &git.CommitOptions{
		Author: &object.Signature{
			Name:  "Stack Sync",
			Email: "sync@stackfilesync.com",
			When:  time.Now(),
		},
	})

	return err
}

// Push pushes commits to remote
func (o *Operations) Push() error {
	auth, _ := getSSHAuth()

	return o.repo.Push(&git.PushOptions{
		RemoteName: "origin",
		Auth:       auth,
		Progress:   os.Stdout,
	})
}

// getSSHAuth returns SSH authentication
func getSSHAuth() (*ssh.PublicKeys, error) {
	homeDir, err := os.UserHomeDir()
	if err != nil {
		return nil, err
	}

	sshKeyPath := filepath.Join(homeDir, ".ssh", "id_rsa")
	auth, err := ssh.NewPublicKeysFromFile("git", sshKeyPath, "")
	if err != nil {
		return nil, err
	}

	return auth, nil
}

// IsGitRepository checks if a path is a git repository
func IsGitRepository(path string) bool {
	_, err := git.PlainOpen(path)
	return err == nil
}

// GetRepositoryURL returns the remote URL of the repository
func GetRepositoryURL(path string) (string, error) {
	repo, err := git.PlainOpen(path)
	if err != nil {
		return "", err
	}

	remote, err := repo.Remote("origin")
	if err != nil {
		return "", err
	}

	config := remote.Config()
	if len(config.URLs) == 0 {
		return "", fmt.Errorf("no remote URL found")
	}

	return config.URLs[0], nil
}

// GetBranchName returns the current branch name
func (o *Operations) GetBranchName() (string, error) {
	head, err := o.repo.Head()
	if err != nil {
		return "", err
	}

	return strings.TrimPrefix(head.Name().String(), "refs/heads/"), nil
}

// CheckoutBranch checks out a specific branch
func (o *Operations) CheckoutBranch(branchName string) error {
	w, err := o.repo.Worktree()
	if err != nil {
		return err
	}

	// Get the branch reference
	branchRef := plumbing.NewBranchReferenceName(branchName)

	// Check if the branch exists locally
	_, err = o.repo.Reference(branchRef, true)
	if err != nil {
		// Branch doesn't exist locally, try to create it from remote
		remoteBranchRef := plumbing.NewRemoteReferenceName("origin", branchName)
		remoteRef, err := o.repo.Reference(remoteBranchRef, true)
		if err != nil {
			return fmt.Errorf("branch not found locally or remotely: %s", branchName)
		}

		// Create local branch tracking the remote branch
		newBranchRef := plumbing.NewHashReference(branchRef, remoteRef.Hash())
		err = o.repo.Storer.SetReference(newBranchRef)
		if err != nil {
			return fmt.Errorf("failed to create local branch: %w", err)
		}
	}

	// Checkout the branch
	err = w.Checkout(&git.CheckoutOptions{
		Branch: branchRef,
	})
	if err != nil {
		return fmt.Errorf("failed to checkout branch: %w", err)
	}

	return nil
}

package ui

import (
	"fmt"
	"strings"
	"time"

	"github.com/fatih/color"
	"github.com/manifoldco/promptui"
	"github.com/stackfilesync/stack-sync-cli/pkg/models"
)

// SelectRepository shows an interactive selector for repositories
func SelectRepository(repos []models.Repository) (*models.Repository, error) {
	if len(repos) == 0 {
		return nil, fmt.Errorf("no repositories configured")
	}

	// Custom templates for the selector
	templates := &promptui.SelectTemplates{
		Label:    "{{ . }}",
		Active:   "▸ {{ .GetDisplayName | cyan }}",
		Inactive: "  {{ .GetDisplayName | faint }}",
		Selected: "✔ {{ .Name | green }}",
		Details: `
--------- Repository Details ----------
{{ "Name:" | faint }}         {{ .Name }}
{{ "URL:" | faint }}          {{ .URL }}
{{ "Local Path:" | faint }}   {{ .LocalPath }}
{{ "Status:" | faint }}       {{ .GetStatusText }}
{{ "Watch Mode:" | faint }}   {{ if .WatchMode }}{{ "Enabled" | green }}{{ else }}{{ "Disabled" | faint }}{{ end }}
{{ "Last Sync:" | faint }}    {{ if .LastSync }}{{ .LastSync.Format "2006-01-02 15:04:05" }}{{ else }}{{ "Never" | faint }}{{ end }}
{{ "Files:" | faint }}        {{ .FilesTracked }} tracked, {{ .FilesModified }} modified`,
	}

	// Searcher function for filtering
	searcher := func(input string, index int) bool {
		repo := repos[index]
		name := strings.Replace(strings.ToLower(repo.Name), " ", "", -1)
		url := strings.Replace(strings.ToLower(repo.URL), " ", "", -1)
		input = strings.Replace(strings.ToLower(input), " ", "", -1)
		return strings.Contains(name, input) || strings.Contains(url, input)
	}

	prompt := promptui.Select{
		Label:     "Select a repository to sync",
		Items:     repos,
		Templates: templates,
		Size:      10,
		Searcher:  searcher,
	}

	idx, _, err := prompt.Run()
	if err != nil {
		return nil, err
	}

	return &repos[idx], nil
}

// ConfirmAction shows a yes/no confirmation prompt
func ConfirmAction(message string) bool {
	prompt := promptui.Prompt{
		Label:     message,
		IsConfirm: true,
	}

	result, err := prompt.Run()
	if err != nil {
		return false
	}

	return result == "y" || result == "Y"
}

// PromptInput shows an input prompt
func PromptInput(label string, defaultValue string) (string, error) {
	prompt := promptui.Prompt{
		Label:   label,
		Default: defaultValue,
	}

	return prompt.Run()
}

// PrintSuccess prints a success message
func PrintSuccess(format string, args ...interface{}) {
	green := color.New(color.FgGreen)
	green.Printf("✓ "+format+"\n", args...)
}

// PrintError prints an error message
func PrintError(format string, args ...interface{}) {
	red := color.New(color.FgRed)
	red.Printf("✗ "+format+"\n", args...)
}

// PrintInfo prints an info message
func PrintInfo(format string, args ...interface{}) {
	cyan := color.New(color.FgCyan)
	cyan.Printf("ℹ "+format+"\n", args...)
}

// PrintWarning prints a warning message
func PrintWarning(format string, args ...interface{}) {
	yellow := color.New(color.FgYellow)
	yellow.Printf("⚠ "+format+"\n", args...)
}

// ShowProgress shows a simple progress indicator
func ShowProgress(message string, done chan bool) {
	frames := []string{"⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"}
	i := 0
	ticker := time.NewTicker(100 * time.Millisecond)
	defer ticker.Stop()

	for {
		select {
		case <-done:
			fmt.Print("\r")
			return
		case <-ticker.C:
			fmt.Printf("\r%s %s", frames[i%len(frames)], message)
			i++
		}
	}
}

// PrintRepositoryList prints a list of repositories in table format
func PrintRepositoryList(repos []models.Repository) {
	if len(repos) == 0 {
		PrintWarning("No repositories configured")
		return
	}

	fmt.Println()
	fmt.Println("Repositories:")
	fmt.Println(strings.Repeat("─", 80))

	for _, repo := range repos {
		statusColor := getStatusColor(repo.Status)
		watchMode := "○"
		if repo.WatchMode {
			watchMode = color.GreenString("●")
		}

		fmt.Printf("  %s %-20s %s %-20s %s\n",
			repo.GetIcon(),
			truncate(repo.Name, 20),
			watchMode,
			statusColor(repo.GetStatusText()),
			color.New(color.Faint).Sprint(truncate(repo.URL, 35)),
		)
	}

	fmt.Println(strings.Repeat("─", 80))
	fmt.Printf("\nTotal: %d repositories\n", len(repos))
	fmt.Println()
}

// getStatusColor returns a color function based on status
func getStatusColor(status models.SyncStatus) func(a ...interface{}) string {
	switch status {
	case models.StatusSyncing:
		return color.New(color.FgCyan).SprintFunc()
	case models.StatusUpToDate:
		return color.New(color.FgGreen).SprintFunc()
	case models.StatusConflict, models.StatusError:
		return color.New(color.FgRed).SprintFunc()
	case models.StatusModified:
		return color.New(color.FgYellow).SprintFunc()
	default:
		return color.New(color.Faint).SprintFunc()
	}
}

// truncate truncates a string to the specified length
func truncate(s string, maxLen int) string {
	if len(s) <= maxLen {
		return s
	}
	return s[:maxLen-3] + "..."
}

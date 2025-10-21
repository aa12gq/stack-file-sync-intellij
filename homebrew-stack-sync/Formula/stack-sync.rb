class StackSync < Formula
  desc "Interactive file synchronization tool for development teams"
  homepage "https://github.com/aa12gq/stack-file-sync-intellij"
  url "https://github.com/aa12gq/stack-file-sync-intellij/releases/download/v1.1.1/stack-sync"
  sha256 "98a22ea439855749b90f6b8b5789993c8e4e57abcaf253c482618ab1177e3a2a"
  license "MIT"
  head "https://github.com/aa12gq/stack-file-sync-intellij.git", branch: "main"

  # This is a pre-compiled binary, no compilation needed
  # No dependencies required for binary installation

  def install
    # This is a pre-compiled binary, just copy it to bin
    bin.install "stack-sync"
  end

  test do
    assert_match "Stack Sync CLI", shell_output("#{bin}/stack-sync --version")
  end
end

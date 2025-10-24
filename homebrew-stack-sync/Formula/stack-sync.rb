class StackSync < Formula
  desc "Interactive file synchronization tool for development teams"
  homepage "https://github.com/aa12gq/stack-file-sync-intellij"
  version "1.1.5"
  license "MIT"
  
  head "https://github.com/aa12gq/stack-file-sync-intellij.git", branch: "main"

  # Use universal binary for macOS (supports both Intel and ARM)
  url "https://github.com/aa12gq/stack-file-sync-intellij/releases/download/v1.1.5/stack-sync-darwin-universal.tar.gz"
  sha256 "1c8f9a7063fd4952f2471deb0f740d596fec7039fd7e9a7ca83a2d72cc9094ce"

  def install
    bin.install "stack-sync"
  end

  test do
    system "#{bin}/stack-sync", "version"
  end
end

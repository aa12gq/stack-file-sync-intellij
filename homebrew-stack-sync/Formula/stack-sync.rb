class StackSync < Formula
  desc "Interactive file synchronization tool for development teams"
  homepage "https://github.com/aa12gq/stack-file-sync-intellij"
  version "1.2.0"
  license "MIT"
  
  head "https://github.com/aa12gq/stack-file-sync-intellij.git", branch: "main"

  # Use universal binary for macOS (supports both Intel and ARM)
  url "https://github.com/aa12gq/stack-file-sync-intellij/releases/download/v1.2.0/stack-sync-darwin-universal.tar.gz"
  sha256 "f2b2cbce9d1c48b0526ca9b7ebe84d5eff7cb090ea928205616a789257a09e65"

  def install
    bin.install "stack-sync"
  end

  test do
    system "#{bin}/stack-sync", "version"
  end
end

class StackSync < Formula
  desc "Interactive file synchronization tool for development teams"
  homepage "https://github.com/stackfilesync/stack-sync-cli"
  url "https://github.com/stackfilesync/stack-sync-cli/archive/v1.1.0.tar.gz"
  sha256 "YOUR_SHA256_HERE"
  license "MIT"
  head "https://github.com/stackfilesync/stack-sync-cli.git", branch: "main"

  depends_on "go" => :build

  def install
    system "make", "build"
    bin.install "build/stack-sync"
  end

  test do
    system "#{bin}/stack-sync", "version"
  end
end

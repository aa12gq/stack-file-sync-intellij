class StackSync < Formula
  desc "Interactive file synchronization tool for development teams"
  homepage "https://github.com/aa12gq/stack-file-sync-intellij"
  url "https://github.com/aa12gq/stack-file-sync-intellij/archive/v1.1.0.tar.gz"
  sha256 "560789bd457ff11cc83c287f610bcdd0c680ce7d6c7f9127324289897b191b1c"
  license "MIT"
  head "https://github.com/aa12gq/stack-file-sync-intellij.git", branch: "main"

  depends_on "go" => :build

  def install
    system "make", "build"
    bin.install "build/stack-sync"
  end

  test do
    system "#{bin}/stack-sync", "version"
  end
end

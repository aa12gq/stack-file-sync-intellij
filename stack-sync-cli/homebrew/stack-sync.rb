class StackSync < Formula
  desc "Interactive file synchronization tool for development teams"
  homepage "https://github.com/aa12gq/stack-file-sync-intellij"
  # 默认使用 universal binary，支持所有 macOS 架构
  url "https://github.com/aa12gq/stack-file-sync-intellij/releases/download/v1.2.0/stack-sync-darwin-universal.tar.gz"
  sha256 "f2b2cbce9d1c48b0526ca9b7ebe84d5eff7cb090ea928205616a789257a09e65"
  license "MIT"
  head "https://github.com/aa12gq/stack-file-sync-intellij.git", branch: "main"

  # 支持多种架构
  on_macos do
    # 优先使用 universal binary，同时支持 Apple Silicon 和 Intel
    # 如果 universal binary 不可用，则回退到架构特定的版本
    on_arm do
      # Apple Silicon 可以使用 universal binary 或 ARM64 特定版本
      # Universal binary 优先，因为它同时支持两种架构
    end
    
    on_intel do
      # Intel Mac 可以使用 universal binary 或 x86_64 特定版本
      # Universal binary 优先，因为它同时支持两种架构
    end
  end

  on_linux do
    on_arm do
      url "https://github.com/aa12gq/stack-file-sync-intellij/releases/download/v1.2.0/stack-sync-linux-arm64.tar.gz"
      sha256 "63bfb23e12ddcecdca321fae7f84cd2bce430ac9e49846d0436941288ae7632b"
    end
    
    on_intel do
      url "https://github.com/aa12gq/stack-file-sync-intellij/releases/download/v1.2.0/stack-sync-linux-amd64.tar.gz"
      sha256 "e61fbd20077cb6acbc6ba26672a85f1a7c2c72e7344f40bca5fc5ad281e84344"
    end
  end

  def install
    if OS.mac?
      # macOS 使用 universal binary，支持 Apple Silicon 和 Intel
      bin.install "stack-sync" => "stack-sync"
    elsif OS.linux?
      # Linux 根据架构选择对应的二进制文件
      if Hardware::CPU.arm?
        bin.install "stack-sync-linux-arm64" => "stack-sync"
      else
        bin.install "stack-sync-linux-amd64" => "stack-sync"
      end
    end
  end

  test do
    system "#{bin}/stack-sync", "--version"
  end
end

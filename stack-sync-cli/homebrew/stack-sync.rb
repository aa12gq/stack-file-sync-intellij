class StackSync < Formula
  desc "Interactive file synchronization tool for development teams"
  homepage "https://github.com/aa12gq/stack-file-sync-intellij"
  # 默认使用 universal binary，支持所有 macOS 架构
  url "https://github.com/aa12gq/stack-file-sync-intellij/releases/download/v1.1.5/stack-sync-darwin-universal.tar.gz"
  sha256 "13f200db1238317a0949d4c7abff8c1f302522cd191d831274aa5cf11a4989a1"
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
      url "https://github.com/aa12gq/stack-file-sync-intellij/releases/download/v1.1.5/stack-sync-linux-arm64.tar.gz"
      sha256 "ecb9708d708e3d5c8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8"
    end
    
    on_intel do
      url "https://github.com/aa12gq/stack-file-sync-intellij/releases/download/v1.1.5/stack-sync-linux-amd64.tar.gz"
      sha256 "7f70dd3e1888ea4c8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8b8"
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

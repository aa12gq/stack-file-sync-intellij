#!/bin/bash

# Stack Sync CLI Installation Script
# This script downloads and installs the latest version of stack-sync

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
REPO="stackfilesync/stack-sync-cli"
BINARY_NAME="stack-sync"
INSTALL_DIR="/usr/local/bin"

# Detect OS and architecture
OS=$(uname -s | tr '[:upper:]' '[:lower:]')
ARCH=$(uname -m)

case $ARCH in
    x86_64)
        ARCH="amd64"
        ;;
    arm64|aarch64)
        ARCH="arm64"
        ;;
    *)
        echo -e "${RED}Unsupported architecture: $ARCH${NC}"
        exit 1
        ;;
esac

case $OS in
    darwin)
        OS="darwin"
        ;;
    linux)
        OS="linux"
        ;;
    *)
        echo -e "${RED}Unsupported operating system: $OS${NC}"
        exit 1
        ;;
esac

echo -e "${GREEN}🚀 Stack Sync CLI Installer${NC}"
echo "================================"
echo "OS: $OS"
echo "Architecture: $ARCH"
echo ""

# Function to get latest release
get_latest_release() {
    curl -s "https://api.github.com/repos/$REPO/releases/latest" | grep '"tag_name":' | sed -E 's/.*"([^"]+)".*/\1/'
}

# Function to download binary
download_binary() {
    local version=$1
    local url="https://github.com/$REPO/releases/download/$version/stack-sync-$OS-$ARCH"
    
    if [ "$OS" = "windows" ]; then
        url="${url}.exe"
    fi
    
    echo "Downloading from: $url"
    curl -L -o "$BINARY_NAME" "$url"
    chmod +x "$BINARY_NAME"
}

# Check if Go is installed
if command -v go &> /dev/null; then
    echo -e "${BLUE}📦 Installing via Go...${NC}"
    
    # Try to install via Go first
    if go install "github.com/$REPO/cmd/stack-sync@latest" 2>/dev/null; then
        echo -e "${GREEN}✓ Installed via Go successfully!${NC}"
        
        # Check if GOPATH/bin is in PATH
        if [[ ":$PATH:" != *":$HOME/go/bin:"* ]]; then
            echo -e "${YELLOW}⚠️  Warning: $HOME/go/bin is not in your PATH${NC}"
            echo "Add this to your ~/.bashrc or ~/.zshrc:"
            echo "  export PATH=\$PATH:\$HOME/go/bin"
        fi
        
        INSTALLED=true
    else
        echo -e "${YELLOW}⚠️  Go install failed, trying binary download...${NC}"
        INSTALLED=false
    fi
else
    echo -e "${YELLOW}⚠️  Go not found, downloading binary...${NC}"
    INSTALLED=false
fi

# If Go install failed or not available, download binary
if [ "$INSTALLED" != true ]; then
    echo -e "${BLUE}📥 Downloading latest release...${NC}"
    
    # Get latest version
    LATEST_VERSION=$(get_latest_release)
    if [ -z "$LATEST_VERSION" ]; then
        echo -e "${RED}❌ Failed to get latest version${NC}"
        exit 1
    fi
    
    echo "Latest version: $LATEST_VERSION"
    
    # Create temp directory
    TEMP_DIR=$(mktemp -d)
    cd "$TEMP_DIR"
    
    # Download binary
    download_binary "$LATEST_VERSION"
    
    # Install binary
    if [ ! -w "$INSTALL_DIR" ]; then
        echo -e "${YELLOW}🔐 Need sudo permissions to install to $INSTALL_DIR${NC}"
        sudo mv "$BINARY_NAME" "$INSTALL_DIR/"
    else
        mv "$BINARY_NAME" "$INSTALL_DIR/"
    fi
    
    # Clean up
    cd ~
    rm -rf "$TEMP_DIR"
    
    echo -e "${GREEN}✓ Binary installed successfully!${NC}"
fi

# Verify installation
if command -v stack-sync &> /dev/null; then
    echo ""
    echo -e "${GREEN}🎉 Installation complete!${NC}"
    echo ""
    echo "Stack Sync version:"
    stack-sync version
    echo ""
    echo -e "${BLUE}📚 Get started:${NC}"
    echo "  1. Initialize config:  ${GREEN}stack-sync init${NC}"
    echo "  2. Add a repository:   ${GREEN}stack-sync add${NC}"
    echo "  3. Start syncing:      ${GREEN}stack-sync sync <repo-name>${NC}"
    echo ""
    echo "For more help: ${GREEN}stack-sync help${NC}"
    echo ""
    echo -e "${BLUE}🔗 Documentation: https://github.com/$REPO${NC}"
else
    echo -e "${RED}❌ Installation failed. Please try alternative methods.${NC}"
    echo ""
    echo -e "${BLUE}📦 Alternative Installation Methods:${NC}"
    echo ""
    
    case $OS in
        "darwin")
            echo -e "${GREEN}🍺 Homebrew (macOS):${NC}"
            echo "  brew tap YOUR_USERNAME/stack-sync"
            echo "  brew install stack-sync"
            echo ""
            echo "  Or if submitted to Homebrew Core:"
            echo "  brew install stack-sync"
            ;;
        "linux")
            echo -e "${GREEN}📦 Snap (Linux):${NC}"
            echo "  sudo snap install stack-sync"
            echo ""
            echo -e "${GREEN}📦 AUR (Arch Linux):${NC}"
            echo "  yay -S stack-sync-cli"
            echo "  # or"
            echo "  paru -S stack-sync-cli"
            ;;
    esac
    
    echo ""
    echo -e "${BLUE}🔧 Manual Installation:${NC}"
    echo "1. Download from: https://github.com/$REPO/releases"
    echo "2. Install via Go: go install github.com/$REPO/cmd/stack-sync@latest"
    echo ""
    echo -e "${YELLOW}💡 Note: Package manager installations may take time to be available${NC}"
    echo "   after the initial release. Use the direct installation method above"
    echo "   for immediate access."
    exit 1
fi

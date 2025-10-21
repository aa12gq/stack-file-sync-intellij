#!/bin/bash

# Stack Sync CLI Installation Script
# This script downloads and installs the latest version of stack-sync

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

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

echo -e "${GREEN}Stack Sync CLI Installer${NC}"
echo "================================"
echo "OS: $OS"
echo "Architecture: $ARCH"
echo ""

# Check if Go is installed
if ! command -v go &> /dev/null; then
    echo -e "${YELLOW}Go is not installed. Installing from source...${NC}"

    # Clone and build
    TEMP_DIR=$(mktemp -d)
    cd "$TEMP_DIR"

    echo "Cloning repository..."
    git clone https://github.com/stackfilesync/stack-sync-cli.git
    cd stack-sync-cli

    echo "Building binary..."
    go build -o stack-sync ./cmd/stack-sync

    # Install binary
    INSTALL_DIR="/usr/local/bin"
    if [ ! -w "$INSTALL_DIR" ]; then
        echo -e "${YELLOW}Need sudo permissions to install to $INSTALL_DIR${NC}"
        sudo mv stack-sync "$INSTALL_DIR/"
    else
        mv stack-sync "$INSTALL_DIR/"
    fi

    # Clean up
    cd ~
    rm -rf "$TEMP_DIR"

    echo -e "${GREEN}✓ Stack Sync installed successfully!${NC}"
else
    echo "Installing via Go..."
    go install github.com/stackfilesync/stack-sync-cli/cmd/stack-sync@latest

    # Check if GOPATH/bin is in PATH
    if [[ ":$PATH:" != *":$HOME/go/bin:"* ]]; then
        echo -e "${YELLOW}Warning: $HOME/go/bin is not in your PATH${NC}"
        echo "Add this to your ~/.bashrc or ~/.zshrc:"
        echo "  export PATH=\$PATH:\$HOME/go/bin"
    fi

    echo -e "${GREEN}✓ Stack Sync installed successfully!${NC}"
fi

# Verify installation
if command -v stack-sync &> /dev/null; then
    echo ""
    echo "Stack Sync version:"
    stack-sync version
    echo ""
    echo -e "${GREEN}Installation complete!${NC}"
    echo ""
    echo "Get started:"
    echo "  1. Initialize config:  stack-sync init"
    echo "  2. Add a repository:   stack-sync add"
    echo "  3. Start syncing:      stack-sync"
    echo ""
    echo "For more help: stack-sync help"
else
    echo -e "${RED}Installation failed. Please install manually.${NC}"
    exit 1
fi

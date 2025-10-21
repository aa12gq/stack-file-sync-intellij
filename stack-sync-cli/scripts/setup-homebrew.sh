#!/bin/bash

# Homebrew Tap Setup Script
# This script helps you set up a Homebrew tap for stack-sync-cli

set -e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${GREEN}🍺 Setting up Homebrew Tap for Stack Sync CLI${NC}"
echo "=================================================="

# Check if we're in the right directory
if [ ! -f "homebrew/stack-sync.rb" ]; then
    echo -e "${RED}Error: homebrew/stack-sync.rb not found. Run this script from the project root.${NC}"
    exit 1
fi

echo -e "\n${BLUE}Step 1: Create Homebrew Tap Repository${NC}"
echo "You need to create a new GitHub repository named 'homebrew-stack-sync'"
echo "Repository should be public and owned by your GitHub account"
echo ""
echo "Visit: https://github.com/new"
echo "Repository name: homebrew-stack-sync"
echo "Description: Homebrew tap for Stack Sync CLI"
echo "Make it public"
echo ""
read -p "Press Enter after creating the repository..."

echo -e "\n${BLUE}Step 2: Clone the tap repository${NC}"
echo "Run these commands:"
echo ""
echo "git clone https://github.com/YOUR_USERNAME/homebrew-stack-sync.git"
echo "cd homebrew-stack-sync"
echo "cp ../stack-sync-cli/homebrew/stack-sync.rb Formula/"
echo "git add Formula/stack-sync.rb"
echo "git commit -m 'Add stack-sync formula'"
echo "git push origin main"
echo ""
read -p "Press Enter after completing these steps..."

echo -e "\n${BLUE}Step 3: Update formula with correct SHA256${NC}"
echo "After creating a release, you need to update the SHA256 hash:"
echo ""
echo "1. Download the source tarball from your release"
echo "2. Calculate SHA256: shasum -a 256 stack-sync-cli-1.1.0.tar.gz"
echo "3. Update the sha256 line in Formula/stack-sync.rb"
echo "4. Commit and push the changes"
echo ""

echo -e "\n${BLUE}Step 4: Test the tap${NC}"
echo "Users can then install with:"
echo ""
echo "brew tap YOUR_USERNAME/stack-sync"
echo "brew install stack-sync"
echo ""

echo -e "\n${BLUE}Alternative: Submit to Homebrew Core${NC}"
echo "For wider distribution, you can submit to Homebrew Core:"
echo ""
echo "1. Fork https://github.com/Homebrew/homebrew-core"
echo "2. Create a new formula in Formula/"
echo "3. Follow Homebrew's contribution guidelines"
echo "4. Submit a pull request"
echo ""

echo -e "${GREEN}✅ Homebrew tap setup instructions complete!${NC}"

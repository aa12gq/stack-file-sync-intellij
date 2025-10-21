#!/bin/bash

# Test script for Stack Sync CLI installation
# This script simulates the installation process without actually installing

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${GREEN}🧪 Testing Stack Sync CLI Installation${NC}"
echo "======================================"

# Test 1: Build process
echo -e "\n${BLUE}Test 1: Building binary${NC}"
make clean
make build
if [ -f "build/stack-sync" ]; then
    echo -e "${GREEN}✓ Build successful${NC}"
else
    echo -e "${RED}✗ Build failed${NC}"
    exit 1
fi

# Test 2: Version check
echo -e "\n${BLUE}Test 2: Version check${NC}"
VERSION=$(./build/stack-sync version)
echo "Version: $VERSION"
if [[ $VERSION == *"1.1.0"* ]]; then
    echo -e "${GREEN}✓ Version correct${NC}"
else
    echo -e "${RED}✗ Version incorrect${NC}"
    exit 1
fi

# Test 3: Help command
echo -e "\n${BLUE}Test 3: Help command${NC}"
if ./build/stack-sync help > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Help command works${NC}"
else
    echo -e "${RED}✗ Help command failed${NC}"
    exit 1
fi

# Test 4: Cross-platform build
echo -e "\n${BLUE}Test 4: Cross-platform build${NC}"
if [ -f "scripts/build.sh" ]; then
    chmod +x scripts/build.sh
    echo "Testing cross-platform build..."
    # Just test the script syntax, don't actually build all platforms
    if bash -n scripts/build.sh; then
        echo -e "${GREEN}✓ Build script syntax valid${NC}"
    else
        echo -e "${RED}✗ Build script syntax invalid${NC}"
        exit 1
    fi
else
    echo -e "${RED}✗ Build script not found${NC}"
    exit 1
fi

# Test 5: Install script syntax
echo -e "\n${BLUE}Test 5: Install script syntax${NC}"
if [ -f "scripts/install.sh" ]; then
    if bash -n scripts/install.sh; then
        echo -e "${GREEN}✓ Install script syntax valid${NC}"
    else
        echo -e "${RED}✗ Install script syntax invalid${NC}"
        exit 1
    fi
else
    echo -e "${RED}✗ Install script not found${NC}"
    exit 1
fi

# Test 6: GitHub Actions workflow
echo -e "\n${BLUE}Test 6: GitHub Actions workflow${NC}"
if [ -f ".github/workflows/release.yml" ]; then
    echo -e "${GREEN}✓ Release workflow exists${NC}"
else
    echo -e "${RED}✗ Release workflow missing${NC}"
    exit 1
fi

echo -e "\n${GREEN}🎉 All tests passed!${NC}"
echo ""
echo -e "${BLUE}Next steps for release:${NC}"
echo "1. Commit all changes"
echo "2. Create git tag: git tag -a v1.1.0 -m 'Release v1.1.0'"
echo "3. Push tag: git push origin v1.1.0"
echo "4. GitHub Actions will automatically create the release"
echo "5. Test installation: curl -fsSL https://raw.githubusercontent.com/stackfilesync/stack-sync-cli/main/scripts/install.sh | bash"

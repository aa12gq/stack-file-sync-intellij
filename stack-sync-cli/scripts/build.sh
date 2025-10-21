#!/bin/bash

# Build script for Stack Sync CLI
# Builds binaries for multiple platforms

set -e

VERSION="1.1.4"
BUILD_DIR="build"
APP_NAME="stack-sync"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${GREEN}Building Stack Sync CLI v${VERSION}${NC}"
echo "========================================"

# Clean build directory
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR"

# Build for current platform
echo -e "\n${YELLOW}Building for current platform...${NC}"
go build -o "$BUILD_DIR/$APP_NAME" ./cmd/stack-sync
echo -e "${GREEN}✓ Built: $BUILD_DIR/$APP_NAME${NC}"
    
# Cross-compile for other platforms
PLATFORMS=(
    "darwin/amd64"
    "darwin/arm64"
    "linux/amd64"
    "linux/arm64"
    "windows/amd64"
)

echo -e "\n${YELLOW}Cross-compiling for all platforms...${NC}"

for PLATFORM in "${PLATFORMS[@]}"; do
    IFS='/' read -r -a PARTS <<< "$PLATFORM"
    GOOS="${PARTS[0]}"
    GOARCH="${PARTS[1]}"

    OUTPUT_NAME="$APP_NAME-$GOOS-$GOARCH"
    if [ "$GOOS" = "windows" ]; then
        OUTPUT_NAME="${OUTPUT_NAME}.exe"
    fi

    echo "Building $GOOS/$GOARCH..."
    env GOOS="$GOOS" GOARCH="$GOARCH" go build -o "$BUILD_DIR/$OUTPUT_NAME" ./cmd/stack-sync
    echo -e "${GREEN}✓ Built: $BUILD_DIR/$OUTPUT_NAME${NC}"
done

echo -e "\n${GREEN}Build complete!${NC}"
echo "Binaries are in the $BUILD_DIR directory:"
ls -lh "$BUILD_DIR"

# Create release archives
echo -e "\n${YELLOW}Creating release archives...${NC}"
cd "$BUILD_DIR"

for file in stack-sync-*; do
    if [[ -f "$file" ]]; then
        tar -czf "${file}.tar.gz" "$file"
        echo -e "${GREEN}✓ Created: ${file}.tar.gz${NC}"
    fi
done

cd ..
echo -e "\n${GREEN}All done!${NC}"

#!/bin/bash

# 构建脚本 - 支持Apple Silicon和Intel芯片
set -e

VERSION=${1:-"1.0.0"}
BUILD_DIR="dist"

echo "🚀 开始构建 stack-sync v${VERSION}"

# 清理之前的构建
rm -rf ${BUILD_DIR}
mkdir -p ${BUILD_DIR}

# 构建目标平台
PLATFORMS=(
    "darwin/amd64"    # Intel Mac
    "darwin/arm64"    # Apple Silicon Mac
    "linux/amd64"     # Linux x86_64
    "linux/arm64"     # Linux ARM64
    "windows/amd64"   # Windows x86_64
)

for platform in "${PLATFORMS[@]}"; do
    IFS='/' read -r os arch <<< "$platform"
    
    echo "📦 构建 ${os}/${arch}..."
    
    output_name="stack-sync"
    if [ "$os" = "windows" ]; then
        output_name="stack-sync.exe"
    fi
    
    output_path="${BUILD_DIR}/stack-sync-${os}-${arch}"
    mkdir -p "$output_path"
    
    GOOS=$os GOARCH=$arch go build \
        -ldflags "-X main.version=${VERSION}" \
        -o "${output_path}/${output_name}" \
        ./cmd/stack-sync
    
    # 复制配置文件（如果存在）
    if [ -d "configs" ]; then
        cp -r configs "${output_path}/"
    fi
    
    # 创建压缩包
    cd ${BUILD_DIR}
    if [ "$os" = "windows" ]; then
        zip -r "stack-sync-${os}-${arch}.zip" "stack-sync-${os}-${arch}"
    else
        tar -czf "stack-sync-${os}-${arch}.tar.gz" "stack-sync-${os}-${arch}"
    fi
    cd ..
    
    echo "✅ ${os}/${arch} 构建完成"
done

# 构建 macOS Universal Binary
echo "📦 构建 macOS Universal Binary..."
universal_dir="${BUILD_DIR}/stack-sync-darwin-universal"
mkdir -p "$universal_dir"

# 构建 ARM64 版本
GOOS=darwin GOARCH=arm64 go build \
    -ldflags "-X main.version=${VERSION}" \
    -o "${universal_dir}/stack-sync-arm64" \
    ./cmd/stack-sync

# 构建 AMD64 版本
GOOS=darwin GOARCH=amd64 go build \
    -ldflags "-X main.version=${VERSION}" \
    -o "${universal_dir}/stack-sync-amd64" \
    ./cmd/stack-sync

# 使用 lipo 创建 universal binary
lipo -create \
    "${universal_dir}/stack-sync-arm64" \
    "${universal_dir}/stack-sync-amd64" \
    -output "${universal_dir}/stack-sync"

# 清理临时文件
rm "${universal_dir}/stack-sync-arm64" "${universal_dir}/stack-sync-amd64"

# 复制配置文件
if [ -d "configs" ]; then
        cp -r configs "${universal_dir}/"
    fi

# 创建压缩包
cd ${BUILD_DIR}
tar -czf "stack-sync-darwin-universal.tar.gz" "stack-sync-darwin-universal"
cd ..

echo "✅ macOS Universal Binary 构建完成"

echo "🎉 所有平台构建完成！"
echo "📁 构建文件位于: ${BUILD_DIR}/"
ls -la ${BUILD_DIR}/

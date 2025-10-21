# Stack Sync CLI
APP_NAME=stack-sync
VERSION=1.1.0

# Build output directory
BUILD_DIR=build

# Go build flags
LDFLAGS=-ldflags "-X main.Version=$(VERSION)"

.PHONY: all build install clean test run help deps

all: deps build ## Build the project

deps: ## Download dependencies
	@echo "Downloading dependencies..."
	go mod download
	go mod tidy

build: ## Build for current platform
	@echo "Building $(APP_NAME)..."
	go build $(LDFLAGS) -o $(BUILD_DIR)/$(APP_NAME) ./cmd/stack-sync
	@echo "Build complete: $(BUILD_DIR)/$(APP_NAME)"

build-all: ## Build for all platforms
	@chmod +x scripts/build.sh
	@./scripts/build.sh

install: build ## Install to /usr/local/bin
	@echo "Installing $(APP_NAME) to /usr/local/bin..."
	@sudo cp $(BUILD_DIR)/$(APP_NAME) /usr/local/bin/
	@echo "Installation complete!"

uninstall: ## Uninstall from /usr/local/bin
	@echo "Uninstalling $(APP_NAME)..."
	@sudo rm -f /usr/local/bin/$(APP_NAME)
	@echo "Uninstallation complete!"

run: build ## Build and run
	@$(BUILD_DIR)/$(APP_NAME)

test: ## Run tests
	@echo "Running tests..."
	go test -v ./...

test-coverage: ## Run tests with coverage
	@echo "Running tests with coverage..."
	go test -v -coverprofile=coverage.out ./...
	go tool cover -html=coverage.out

clean: ## Clean build artifacts
	@echo "Cleaning..."
	@rm -rf $(BUILD_DIR)
	@rm -f coverage.out
	@echo "Clean complete!"

fmt: ## Format code
	@echo "Formatting code..."
	@go fmt ./...

lint: ## Run linter
	@echo "Running linter..."
	@golangci-lint run

dev: ## Run in development mode
	@go run ./cmd/stack-sync

help: ## Show this help message
	@echo "Stack Sync CLI - Makefile commands:"
	@echo ""
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2}'

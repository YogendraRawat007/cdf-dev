.PHONY: build install clean test help update quick-install

INSTALL_PATH := /usr/local/bin

help: ## Show this help message
	@echo 'Usage: make [target]'
	@echo ''
	@echo 'Available targets:'
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)

build: ## Build the native binary
	@echo "Building native binary..."
	./gradlew nativeCompile
	@echo "Binary built at: build/native/nativeCompile/cdf-dev"

build-jar: ## Build JAR (for testing without GraalVM)
	@echo "Building JAR..."
	@if /usr/libexec/java_home -v 21 &> /dev/null; then \
		export JAVA_HOME=$$(/usr/libexec/java_home -v 21) && ./gradlew build; \
	else \
		./gradlew build; \
	fi
	@echo "JAR built at: build/libs/cdf-dev-1.0.0.jar"

run: ## Run the application (JAR)
	./gradlew run --args="$(ARGS)"

install: build ## Install the binary to /usr/local/bin
	@echo "Installing cdf-dev to $(INSTALL_PATH)..."
	sudo cp build/native/nativeCompile/cdf-dev $(INSTALL_PATH)/cdf-dev
	sudo chmod +x $(INSTALL_PATH)/cdf-dev
	@echo "cdf-dev installed successfully!"
	@echo "Run 'cdf-dev --help' to get started"

uninstall: ## Uninstall the binary
	@echo "Uninstalling cdf-dev..."
	sudo rm -f $(INSTALL_PATH)/cdf-dev
	@echo "cdf-dev uninstalled"

clean: ## Clean build artifacts
	./gradlew clean
	rm -rf build/

test: build ## Test the binary
	@echo "Testing binary..."
	./build/native/nativeCompile/cdf-dev --help

dev: build-jar ## Run in development mode (faster)
	java -jar build/libs/cdf-dev-1.0.0.jar $(ARGS)

quick-install: ## Quick install using install.sh (recommended)
	@echo "Running install script..."
	./install.sh

update: build-jar quick-install ## Update installed version (build + reinstall)
	@echo "cdf-dev updated successfully!"
	@echo "Test with: cdf-dev --help"

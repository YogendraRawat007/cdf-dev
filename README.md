# cdf-dev

A fast, native CLI tool for deploying and managing CDF services on dev clusters.

## Features

- **Native binary** - No JVM startup time, instant execution
- **Deploy** - Build, push, and deploy with one command
- **Env Management** - Easily patch environment variables
- **Logs** - Stream logs from deployments
- **Port Forwarding** - Quick local access to services
- **Rollback** - Revert to previous versions
- **Config-driven** - Service-specific `.cdf-dev.yaml` files

## Prerequisites

- **GraalVM 21+** (for building native binary)
- **kubectl** configured with access to clusters
- **bazelisk** (for services using Bazel)

## Installation

### Quick Start (JAR - No GraalVM Required)

The tool is ready to use via the JAR file:

```bash
# Create an alias for convenience (add to ~/.zshrc or ~/.bashrc)
alias cdf-dev='java -jar ~/Desktop/cdf-dev/build/libs/cdf-dev-1.0.0.jar'
```

### Option 1: Native Binary (Faster, Requires GraalVM)

```bash
# Install GraalVM first (see "Building Native Binary" section below)

# Build and install native binary
make install

# Verify installation
cdf-dev --help
```

### Option 2: Build and Run JAR (Recommended)

```bash
# Build and install
./install.sh

# Verify installation
cdf-dev --help
```

## Quick Start

1. **Create a `.cdf-dev.yaml`** in your service directory:

```yaml
service:
  name: my-service
  namespace: my-namespace
  deployment: my-service

build:
  type: bazel
  target: //path/to/service:image
  pushTarget: //path/to/service:image.push

cluster:
  context: az-arn-dev-002
  registry: europe-docker.pkg.dev/cognitedata-development/cdf/infrastructure

ports:
  api: 8080
  metrics: 9090

envPresets:
  disable_otel:
    - OTEL_METRICS_EXPORTER=none
    - OTEL_TRACES_EXPORTER=none
```

2. **Deploy your service:**

```bash
cd your-service
cdf-dev deploy
```

## Usage

### Deploy
```bash
# Full deploy (build + push + deploy)
cdf-dev deploy

# Skip build step
cdf-dev deploy --skip-build

# Deploy without waiting
cdf-dev deploy --no-wait
```

### Environment Variables
```bash
# Set single env var
cdf-dev env set LOG_LEVEL=DEBUG

# Set multiple
cdf-dev env set KEY1=value1 KEY2=value2

# Apply preset from config
cdf-dev env preset disable_otel
```

### Logs
```bash
# Show last 100 lines
cdf-dev logs

# Follow logs
cdf-dev logs -f

# Show specific number of lines
cdf-dev logs --tail=50
```

### Port Forward
```bash
# Use defaults from config
cdf-dev port-forward

# Custom ports
cdf-dev port-forward 8080:8080
```

### Rollback
```bash
cdf-dev rollback
```

## Development

```bash
# Build JAR (faster for development)
make build-jar

# Run in dev mode
make dev ARGS="logs --tail=10"

# Build native binary
make build

# Test
make test

# Clean
make clean
```

## Building Native Binary

Requires GraalVM:

```bash
# Install GraalVM (macOS)
brew install graalvm-jdk@21

# Set JAVA_HOME (add to ~/.zshrc or ~/.bashrc)
export JAVA_HOME=/Library/Java/JavaVirtualMachines/graalvm-jdk-21/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"

# Verify GraalVM installation
java -version  # Should show GraalVM

# Build native binary
make build

# Install globally
make install
```

**Note**: The JAR version works perfectly without GraalVM. Native compilation is optional and provides faster startup time.

## Configuration

The tool searches for `.cdf-dev.yaml` in the current directory and parent directories.

See example configs in `examples/` directory.

## Troubleshooting

### "No .cdf-dev.yaml found"
Make sure you're in a directory with a `.cdf-dev.yaml` file or its parent directories.

### Native build fails
Ensure you have GraalVM installed and `JAVA_HOME` is set correctly.

### kubectl commands fail
Check your kubectl context: `kubectl config current-context`

## License

Internal use only - Cognite ASA

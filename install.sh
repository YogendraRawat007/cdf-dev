#!/bin/bash

set -e

echo "Installing cdf-dev CLI..."

# Detect OS
OS=$(uname -s | tr '[:upper:]' '[:lower:]')
ARCH=$(uname -m)

# Set install directory
INSTALL_DIR="${HOME}/.local/bin"
CDF_DEV_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Create install directory if it doesn't exist
mkdir -p "$INSTALL_DIR"

echo "Building cdf-dev..."
cd "$CDF_DEV_DIR"

# Use Java 21 if available (Java 25 has compatibility issues)
if /usr/libexec/java_home -v 21 &> /dev/null; then
    export JAVA_HOME=$(/usr/libexec/java_home -v 21)
    echo "Using Java 21 for build..."
fi

./gradlew build -q

# Create wrapper script
echo "Creating wrapper script..."
cat > "$INSTALL_DIR/cdf-dev" << 'EOF'
#!/bin/bash
# Run with warning suppression for Java 21+
java --enable-native-access=ALL-UNNAMED -jar "$HOME/.cdf-dev/cdf-dev-1.0.0.jar" "$@" 2> >(grep -v "WARNING:" >&2)
EOF

# Make it executable
chmod +x "$INSTALL_DIR/cdf-dev"

# Copy JAR to home directory
mkdir -p "$HOME/.cdf-dev"
cp "$CDF_DEV_DIR/build/libs/cdf-dev-1.0.0.jar" "$HOME/.cdf-dev/"

echo "cdf-dev installed to $INSTALL_DIR/cdf-dev"

# Check if directory is in PATH
if [[ ":$PATH:" != *":$INSTALL_DIR:"* ]]; then
    echo ""
    echo " [WARN] $INSTALL_DIR is not in your PATH"
    echo "   Add this to your ~/.zshrc or ~/.bashrc:"
    echo ""
    echo "   export PATH=\"\$HOME/.local/bin:\$PATH\""
    echo ""
else
    echo ""
    echo "Installation complete! Try: cdf-dev --help"
fi

# Test installation
if command -v cdf-dev &> /dev/null; then
    echo ""
    cdf-dev --help
fi

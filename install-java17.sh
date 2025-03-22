#!/bin/bash
# Script to download and install Java 17 for macOS

echo "Installing Java 17 for macOS..."

# Check if Homebrew is installed
if ! command -v brew &> /dev/null; then
    echo "Homebrew is not installed. Installing Homebrew..."
    /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
    
    # Add Homebrew to PATH for Apple Silicon Macs
    if [[ $(uname -m) == 'arm64' ]]; then
        echo 'eval "$(/opt/homebrew/bin/brew shellenv)"' >> ~/.zprofile
        eval "$(/opt/homebrew/bin/brew shellenv)"
    fi
fi

# Install Java 17 using Homebrew
echo "Installing Java 17 using Homebrew..."
brew install openjdk@17

# Create a symlink to make it available system-wide
if [[ $(uname -m) == 'arm64' ]]; then
    # For Apple Silicon
    JAVA_HOME="/opt/homebrew/opt/openjdk@17"
    sudo ln -sfn "$JAVA_HOME" /Library/Java/JavaVirtualMachines/openjdk-17
else
    # For Intel Macs
    JAVA_HOME="/usr/local/opt/openjdk@17"
    sudo ln -sfn "$JAVA_HOME" /Library/Java/JavaVirtualMachines/openjdk-17
fi

# Verify installation
if [ -x "$JAVA_HOME/bin/java" ]; then
    echo "Java 17 installation verified at: $JAVA_HOME"
    
    # Create gradle.properties to use the new Java installation
    echo "Creating gradle.properties to use Java 17..."
    cat > gradle.properties << EOF
# Gradle settings for Java compatibility
org.gradle.java.home=$JAVA_HOME
org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError
EOF
    
    echo "gradle.properties created with Java 17 configuration."
    
    # Clean Gradle cache
    echo "Cleaning Gradle cache..."
    rm -rf ~/.gradle/caches/*
    echo "Gradle cache cleaned."
    
    # Now run the tests
    echo "Running tests with Java 17..."
    ./gradlew :backend:data:test --tests "io.tolgee.example.OptimizedRepositoryTest" --no-daemon
    
    # Check if the tests passed
    if [ $? -eq 0 ]; then
        echo "Tests passed successfully!"
    else
        echo "Tests failed with exit code $?"
    fi
else
    echo "Java 17 installation could not be verified. Please install Java 17 manually."
fi 
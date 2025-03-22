#!/bin/bash

# Check if Homebrew is installed
if ! command -v brew &> /dev/null; then
    echo "Homebrew not found. Installing Homebrew..."
    /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
fi

# Check if Java 17 is installed
if ! /usr/libexec/java_home -v 17 &> /dev/null; then
    echo "Java 17 not found. Installing OpenJDK 17..."
    brew install openjdk@17
    
    # Create a symlink to make it available to the system
    sudo ln -sfn /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-17.jdk
fi

# Set JAVA_HOME to Java 17
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
echo "Using Java from: $JAVA_HOME"

# Navigate to the backend directory
cd backend

# Run Gradle with the specified arguments
./gradlew "$@" 
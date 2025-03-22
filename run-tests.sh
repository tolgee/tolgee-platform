#!/bin/bash
# Script to run the tests

# Set Java version to 17 for this script
export JAVA_VERSION=17

# Check if Java 17 is installed via Homebrew
if [ -d "/opt/homebrew/opt/openjdk@17" ]; then
    export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
elif [ -d "/usr/local/opt/openjdk@17" ]; then
    export JAVA_HOME="/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
else
    # Try to find Java 17 using /usr/libexec/java_home
    JAVA_HOME=$(/usr/libexec/java_home -v 17 2>/dev/null)
    if [ $? -ne 0 ]; then
        echo "Java 17 not found. Please install it using:"
        echo "brew install openjdk@17"
        exit 1
    fi
fi

echo "Using Java from: $JAVA_HOME"

# Navigate to the backend directory
cd backend

# Run Gradle with the specified arguments
./gradlew "$@"

# Check if the tests passed
if [ $? -eq 0 ]; then
  echo "Tests passed successfully!"
else
  echo "Tests failed with exit code $?"
fi 
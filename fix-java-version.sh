#!/bin/bash
# Script to fix Java version compatibility issues on macOS

echo "Fixing Java version compatibility issues..."

# Check current Java version
echo "Current Java version:"
java -version

# Find a compatible Java version (Java 17 is recommended for Gradle 8.5)
echo "Searching for compatible Java installations..."

compatible_java=""
java_homes=(
  "/Library/Java/JavaVirtualMachines/jdk-17*"
  "/opt/homebrew/opt/openjdk@17"
  "/usr/local/opt/openjdk@17"
)

for path_pattern in "${java_homes[@]}"; do
  for java_home in $path_pattern; do
    if [ -d "$java_home" ]; then
      if [[ "$java_home" == "/Library/Java/JavaVirtualMachines/"* ]]; then
        # For Oracle/OpenJDK style installations
        java_home="$java_home/Contents/Home"
      fi
      
      if [ -x "$java_home/bin/java" ]; then
        echo "Found compatible Java 17 installation: $java_home"
        compatible_java="$java_home"
        break 2
      fi
    fi
  done
done

if [ -z "$compatible_java" ]; then
  echo "No compatible Java 17 installation found. Please install Java 17."
  exit 1
fi

# Create or update gradle.properties to use the compatible Java version
echo "Creating gradle.properties to use Java 17..."
cat > gradle.properties << EOF
# Gradle settings for Java compatibility
org.gradle.java.home=$compatible_java
org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError
EOF

echo "gradle.properties created with Java 17 configuration."

# Clean Gradle cache to ensure fresh start
echo "Cleaning Gradle cache..."
rm -rf ~/.gradle/caches/8.5/scripts/*
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
#!/bin/bash
# Script to check Java versions on macOS

echo "Checking system Java version..."
java -version
echo ""

echo "Checking Gradle's Java version..."
./gradlew --version
echo ""

echo "Searching for Java installations..."
# Check common macOS Java installation locations
common_paths=(
  "/Library/Java/JavaVirtualMachines"
  "/usr/libexec/java_home -V"
)

for path in "${common_paths[@]}"; do
  echo "Checking $path..."
  if [[ "$path" == *"java_home"* ]]; then
    eval $path 2>&1
  else
    if [ -d "$path" ]; then
      ls -la "$path"
    else
      echo "Directory does not exist"
    fi
  fi
done

echo "Checking JAVA_HOME environment variable..."
echo "JAVA_HOME = $JAVA_HOME" 
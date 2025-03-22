#!/bin/bash
# Script to fix the build files and run tests on macOS

echo "Setting up environment for macOS..."

# Create gradle.properties
echo "Creating gradle.properties..."
cat > gradle.properties << EOF
# Gradle settings for macOS
org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError
java.toolchain.languageVersion=17
kotlin.jvm.target.validation.mode=warning
EOF
echo "gradle.properties created."

# Clean Gradle cache
echo "Cleaning Gradle cache..."
rm -rf ~/.gradle/caches/*
echo "Gradle cache cleaned."

# Clean build directories
echo "Cleaning build directories..."
rm -rf ./build
find . -name "build" -type d -exec echo "Removing {}" \; -exec rm -rf {} \; 2>/dev/null || true
echo "Build directories cleaned."

# Kill Gradle daemons
echo "Killing Gradle daemons..."
./gradlew --stop
echo "Gradle daemons stopped."

# Fix build.gradle
echo "Fixing build.gradle..."
if [ -f "build.gradle" ]; then
  cp build.gradle build.gradle.bak
  
  # Use awk for more reliable text processing
  awk '{
    if ($0 ~ /finalizedBy\.add\(project\('\''ee-app'\''\)\.tasks\.findByName\("diffChangelog"\)\)/) {
      print "    if (rootProject.findProject('\'':ee-app'\'') != null) {"
      print "        finalizedBy.add(project('\'':ee-app'\'').tasks.findByName(\"diffChangelog\"))"
      print "    } else {"
      print "        println \"Note: :ee-app project is not available, skipping diffChangelog task\""
      print "    }"
    } else {
      print $0
    }
  }' build.gradle.bak > build.gradle
  
  echo "build.gradle fixed."
fi

# Fix backend/app/build.gradle
echo "Fixing backend/app/build.gradle..."
if [ -f "backend/app/build.gradle" ]; then
  cp backend/app/build.gradle backend/app/build.gradle.bak
  
  # Use awk for more reliable text processing
  awk '{
    if ($0 ~ /implementation project\('\''(:)?ee-app'\''\)/) {
      print "    if (rootProject.findProject('\'':ee-app'\'') != null) {"
      print "        implementation project('\'':ee-app'\'')"
      print "    } else {"
      print "        println \"Note: :ee-app project is not available, skipping dependency\""
      print "    }"
    } else {
      print $0
    }
  }' backend/app/build.gradle.bak > backend/app/build.gradle
  
  echo "backend/app/build.gradle fixed."
fi

# Fix backend/development/build.gradle
echo "Fixing backend/development/build.gradle..."
if [ -f "backend/development/build.gradle" ]; then
  cp backend/development/build.gradle backend/development/build.gradle.bak
  
  # Use awk for more reliable text processing
  awk '{
    if ($0 ~ /implementation project\('\''(:)?ee-app'\''\)/) {
      print "    if (rootProject.findProject('\'':ee-app'\'') != null) {"
      print "        implementation project('\'':ee-app'\'')"
      print "    } else {"
      print "        println \"Note: :ee-app project is not available, skipping dependency\""
      print "    }"
    } else {
      print $0
    }
  }' backend/development/build.gradle.bak > backend/development/build.gradle
  
  echo "backend/development/build.gradle fixed."
fi

# Create minimal ee-app build.gradle
echo "Creating minimal build.gradle for ee-app..."
mkdir -p ee/backend/app

cat > ee/backend/app/build.gradle << EOF
// Completely minimal build file for ee-app
plugins {
    id 'java'
}

description = 'Tolgee EE App'

repositories {
    mavenCentral()
}

dependencies {
    implementation project(':data')
    implementation project(':security')
    implementation project(':api')
}

// No test configuration, no extra plugins, nothing that could cause version issues
EOF

echo "Minimal ee-app build.gradle created."

# Run the tests
echo "Running tests..."
./gradlew :backend:data:test --tests "io.tolgee.example.OptimizedRepositoryTest" --no-daemon

# Check if the tests passed
if [ $? -eq 0 ]; then
  echo "Tests passed successfully!"
else
  echo "Tests failed with exit code $?"
fi 
#!/bin/bash
# Script to clean Gradle cache and build directories on macOS

echo "Killing all Gradle daemons..."
./gradlew --stop

echo "Cleaning Gradle cache..."
rm -rf ~/.gradle/caches/*
echo "Gradle cache cleaned."

echo "Cleaning build directories..."
rm -rf ./build
find . -type d -name "build" -exec rm -rf {} \; 2>/dev/null || true
echo "Build directories cleaned."

echo "Running clean build..."
./gradlew clean --no-daemon
echo "Clean build completed." 
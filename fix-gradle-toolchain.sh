#!/bin/bash
# Script to fix Gradle toolchain issues

echo "Fixing Gradle toolchain issues..."

# Create a gradle.properties file with toolchain configuration
echo "Creating gradle.properties with toolchain configuration..."
cat > gradle.properties << EOF
# Gradle settings for toolchain support
org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError
org.gradle.java.installations.auto-download=true
org.gradle.toolchains.foojay-resolver-convention.enabled=true
EOF

# Create a settings.gradle file with the toolchain resolver plugin
if [ -f "settings.gradle" ]; then
    echo "Updating settings.gradle with toolchain resolver..."
    if ! grep -q "id 'org.gradle.toolchains.foojay-resolver-convention'" settings.gradle; then
        # Create a temporary file
        TEMP_FILE=$(mktemp)
        
        # Add the plugin to settings.gradle
        if grep -q "plugins\s*{" settings.gradle; then
            # Add to existing plugins block
            sed '/plugins\s*{/a \    id "org.gradle.toolchains.foojay-resolver-convention" version "0.7.0"' settings.gradle > "$TEMP_FILE"
        else
            # Add new plugins block at the beginning
            echo 'plugins {
    id "org.gradle.toolchains.foojay-resolver-convention" version "0.7.0"
}

' > "$TEMP_FILE"
            cat settings.gradle >> "$TEMP_FILE"
        fi
        
        # Replace the original file
        mv "$TEMP_FILE" settings.gradle
    fi
fi

# Update build.gradle to use Java toolchain
if [ -f "build.gradle" ]; then
    echo "Updating build.gradle with Java toolchain configuration..."
    
    # Check if toolchain configuration is already present
    if ! grep -q "java\s*{\s*toolchain\s*{" build.gradle; then
        # Create a temporary file
        TEMP_FILE=$(mktemp)
        
        # Add toolchain configuration
        TOOLCHAIN_BLOCK='
allprojects {
    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(17)
        }
    }
}
'
        # Add after plugins block or at the beginning
        if grep -q "plugins\s*{" build.gradle; then
            awk -v block="$TOOLCHAIN_BLOCK" '
                /plugins\s*{/ {
                    print $0;
                    in_block = 1;
                    next;
                }
                in_block && /}/ {
                    print $0;
                    print block;
                    in_block = 0;
                    next;
                }
                { print $0; }
            ' build.gradle > "$TEMP_FILE"
        else
            echo "$TOOLCHAIN_BLOCK" > "$TEMP_FILE"
            cat build.gradle >> "$TEMP_FILE"
        fi
        
        # Replace the original file
        mv "$TEMP_FILE" build.gradle
    fi
fi

# Clean Gradle cache
echo "Cleaning Gradle cache..."
rm -rf ~/.gradle/caches/*
echo "Gradle cache cleaned."

# Stop any running Gradle daemons
echo "Stopping Gradle daemons..."
./gradlew --stop
echo "Gradle daemons stopped."

# Run the tests with toolchain support
echo "Running tests with Java 17 toolchain..."
./gradlew :backend:data:test --tests "io.tolgee.example.OptimizedRepositoryTest" --no-daemon

# Check if the tests passed
if [ $? -eq 0 ]; then
    echo "Tests passed successfully!"
else
    echo "Tests failed with exit code $?"
fi 
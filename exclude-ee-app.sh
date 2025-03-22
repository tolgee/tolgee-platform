#!/bin/bash
# Script to exclude ee-app from settings.gradle

echo "Excluding ee-app from settings.gradle..."

# Create a backup of settings.gradle
cp settings.gradle settings.gradle.bak
echo "Original settings.gradle backed up to settings.gradle.bak"

# Comment out any line that includes ee-app
sed -i.tmp 's/include.*ee-app.*/\/\/ &  \/\/ Temporarily disabled/' settings.gradle
rm settings.gradle.tmp
echo "ee-app excluded from settings.gradle"

# Create a minimal build.gradle for ee-app
ee_app_dir="ee/backend/app"
if [ -d "$ee_app_dir" ]; then
  echo "Creating minimal build.gradle for ee-app..."
  
  # Backup original build.gradle
  if [ -f "$ee_app_dir/build.gradle" ]; then
    cp "$ee_app_dir/build.gradle" "$ee_app_dir/build.gradle.bak"
  fi
  
  # Create minimal build.gradle
  cat > "$ee_app_dir/build.gradle" << EOF
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
EOF
  
  echo "Created minimal build.gradle for ee-app"
fi 
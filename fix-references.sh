#!/bin/bash
# Script to fix references to ee-app in build files

echo "Fixing references to ee-app in build files..."

# Fix backend/app/build.gradle
app_build_file="backend/app/build.gradle"
if [ -f "$app_build_file" ]; then
  echo "Fixing $app_build_file..."
  
  # Create a backup
  cp "$app_build_file" "$app_build_file.bak"
  
  # Replace direct references to :ee-app with conditional checks
  sed -i.tmp 's/implementation project(..:ee-app..)/if (rootProject.findProject(":ee-app") != null) { implementation project(":ee-app") } else { println "Note: :ee-app project is not available, skipping dependency" }/' "$app_build_file"
  rm "$app_build_file.tmp"
  
  echo "$app_build_file updated."
fi

# Fix backend/development/build.gradle
dev_build_file="backend/development/build.gradle"
if [ -f "$dev_build_file" ]; then
  echo "Fixing $dev_build_file..."
  
  # Create a backup
  cp "$dev_build_file" "$dev_build_file.bak"
  
  # Replace direct references to :ee-app with conditional checks
  sed -i.tmp 's/implementation project(..:ee-app..)/if (rootProject.findProject(":ee-app") != null) { implementation project(":ee-app") } else { println "Note: :ee-app project is not available, skipping dependency" }/' "$dev_build_file"
  rm "$dev_build_file.tmp"
  
  echo "$dev_build_file updated."
fi

# Fix root build.gradle
root_build_file="build.gradle"
if [ -f "$root_build_file" ]; then
  echo "Fixing $root_build_file..."
  
  # Create a backup
  cp "$root_build_file" "$root_build_file.bak"
  
  # Replace the specific line that's causing the error
  sed -i.tmp 's/finalizedBy\.add(project(..:ee-app..)\.tasks\.findByName("diffChangelog"))/if (rootProject.findProject(":ee-app") != null) { finalizedBy.add(project(":ee-app").tasks.findByName("diffChangelog")) } else { println "Note: :ee-app project is not available, skipping diffChangelog task" }/' "$root_build_file"
  rm "$root_build_file.tmp"
  
  echo "$root_build_file updated."
fi 
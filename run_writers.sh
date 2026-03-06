#!/bin/bash

# Configuration
JAVAFX_SDK="/Users/Unat/javafx-sdk-21.0.9"
MODULE_PATH="$JAVAFX_SDK/lib"
MODULES="javafx.controls,javafx.fxml,javafx.graphics,javafx.media,javafx.web,java.sql"

# Paths
SRC_DIR="src"
BIN_DIR="bin"

# Clear bin
rm -rf "$BIN_DIR"/*
mkdir -p "$BIN_DIR"

# Compile
echo "Building WritersAssist Ecosystem..."
find "$SRC_DIR" -name "*.java" > sources.txt
javac --module-path "$MODULE_PATH" --add-modules "$MODULES" -cp "lib/sqlite-jdbc.jar" -d "$BIN_DIR" @sources.txt
rm sources.txt

# Copy Resources
mkdir -p "$BIN_DIR/com/writersassist/ui/css"
cp "$SRC_DIR/com/writersassist/ui/css/style.css" "$BIN_DIR/com/writersassist/ui/css/style.css"

echo "Build Dynamic complete."

echo "Select Mode:"
echo "1) Cloud Backend (RMI Server)"
echo "2) WritersAssist App"
echo "3) Hybrid Launch (Start both)"
read -p "Choice: " choice

if [ "$choice" == "1" ]; then
    java -cp "$BIN_DIR:lib/sqlite-jdbc.jar" com.writersassist.lab7.ScriptServerLauncher
elif [ "$choice" == "2" ]; then
    java --module-path "$MODULE_PATH" --add-modules "$MODULES" -cp "$BIN_DIR:lib/sqlite-jdbc.jar" com.writersassist.view.App
elif [ "$choice" == "3" ]; then
    java -cp "$BIN_DIR:lib/sqlite-jdbc.jar" com.writersassist.lab7.ScriptServerLauncher &
    sleep 2
    java --module-path "$MODULE_PATH" --add-modules "$MODULES" -cp "$BIN_DIR:lib/sqlite-jdbc.jar" com.writersassist.view.App
fi

#!/bin/bash
echo "================================================"
echo "  DocVerify — Java Backend Build Script"
echo "================================================"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR/backend"

mkdir -p out

echo "Compiling Java sources..."
javac -d out src/main/java/com/ddvs/*.java
if [ $? -ne 0 ]; then
    echo ""
    echo "[ERROR] Compilation failed. Make sure Java JDK 11+ is installed."
    echo "  macOS:  brew install openjdk@17"
    echo "  Ubuntu: sudo apt install openjdk-17-jdk"
    exit 1
fi

echo ""
echo "[OK] Compilation successful!"
echo ""
echo "Starting DocVerify backend on http://localhost:8080"
echo "Open frontend/index.html in your browser."
echo "Press Ctrl+C to stop."
echo ""
java -cp out com.ddvs.Main

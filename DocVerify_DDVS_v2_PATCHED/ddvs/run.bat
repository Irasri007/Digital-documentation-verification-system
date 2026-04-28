@echo off
echo ================================================
echo   DocVerify — Java Backend Build Script
echo ================================================

cd /d "%~dp0backend"

if not exist out mkdir out

echo Compiling Java sources...
javac -cp ".;..\lib/*" src/main/java/com/ddvs/*.java

if errorlevel 1 (
    echo.
    echo [ERROR] Compilation failed. Make sure Java JDK 11+ is installed.
    echo Download from: https://adoptium.net/
    pause
    exit /b 1
)

echo.
echo [OK] Compilation successful!
echo.
echo Starting DocVerify backend on http://localhost:8080
echo Open frontend\index.html in your browser.
echo Press Ctrl+C to stop the server.
echo.

java -cp ".;..\lib/*;src/main/java" com.ddvs.Main
pause
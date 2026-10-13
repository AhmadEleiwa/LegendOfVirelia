@echo off
setlocal

set "PROJECT_DIR=%~dp0.."
call "%PROJECT_DIR%\gradlew.bat" -p "%PROJECT_DIR%" launcherJar
if errorlevel 1 (
    echo.
    echo Failed to build the launcher. Check the Gradle output above.
    pause
    exit /b 1
)

java -jar "%~dp0Launcher-1.0-SNAPSHOT-all.jar"
if errorlevel 1 (
    echo.
    echo The launcher could not start. Make sure Java 17 or newer is installed.
    pause
    exit /b 1
)

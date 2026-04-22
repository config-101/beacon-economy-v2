@echo off
setlocal
cd /d "%~dp0"

if exist "C:\Gradle\bin\gradle.bat" (
    "C:\Gradle\bin\gradle.bat" build
    goto :end
)

where gradle >nul 2>nul
if %errorlevel%==0 (
    gradle build
    goto :end
)

echo Gradle was not found.
echo Since your Gradle path is C:\Gradle\bin, make sure this file exists:
echo C:\Gradle\bin\gradle.bat
echo.
echo If it exists, run this manually inside this folder:
echo C:\Gradle\bin\gradle.bat build
pause
exit /b 1

:end
if %errorlevel% neq 0 (
    echo.
    echo Build failed. Copy the error above and send it to ChatGPT.
    pause
    exit /b %errorlevel%
)

echo.
echo Build complete. Your mod jar should be in build\libs\
pause

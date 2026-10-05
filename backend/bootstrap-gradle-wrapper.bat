@echo off
setlocal EnableExtensions

rem Resolve the backend directory WITHOUT a trailing backslash.
for %%I in ("%~dp0.") do set "BACKEND_DIR=%%~fI"
set "GRADLE_VERSION=8.14.5"

echo ============================================================
echo  OOAD Backend - Gradle Wrapper Bootstrap
echo ============================================================
echo Backend dir   : %BACKEND_DIR%
echo Gradle target : %GRADLE_VERSION%
echo.

if exist "%BACKEND_DIR%\gradlew.bat" (
    echo Gradle wrapper already exists.
    call "%BACKEND_DIR%\gradlew.bat" --version
    set "RC=%ERRORLEVEL%"
    if not "%RC%"=="0" (
        echo.
        echo ERROR: Existing Gradle wrapper returned exit code %RC%.
    )
    exit /b %RC%
)

where gradle >nul 2>nul
if %ERRORLEVEL%==0 (
    echo Using global Gradle to generate wrapper %GRADLE_VERSION%...
    call gradle -p "%BACKEND_DIR%" wrapper --gradle-version %GRADLE_VERSION%
    set "RC=%ERRORLEVEL%"
    if not "%RC%"=="0" (
        echo.
        echo ERROR: Global Gradle failed with exit code %RC%.
        echo Run this command manually to inspect the full error:
        echo   gradle -p "%BACKEND_DIR%" wrapper --gradle-version %GRADLE_VERSION%
    )
    exit /b %RC%
)

set "OLD_WRAPPER=%BACKEND_DIR%\..\..\FileShare-P2P\gradlew.bat"
for %%I in ("%OLD_WRAPPER%") do set "OLD_WRAPPER=%%~fI"

if exist "%OLD_WRAPPER%" (
    echo Global Gradle not found.
    echo Reusing FileShare-P2P wrapper only to generate this project's own wrapper...
    call "%OLD_WRAPPER%" -p "%BACKEND_DIR%" wrapper --gradle-version %GRADLE_VERSION%
    set "RC=%ERRORLEVEL%"
    if not "%RC%"=="0" (
        echo.
        echo ERROR: FileShare-P2P wrapper failed with exit code %RC%.
    )
    exit /b %RC%
)

echo ERROR: Cannot find a Gradle installation or the previous FileShare-P2P wrapper.
echo.
echo Expected old wrapper at:
echo   %OLD_WRAPPER%
echo.
echo Alternative:
echo   gradle -p "%BACKEND_DIR%" wrapper --gradle-version %GRADLE_VERSION%
exit /b 1

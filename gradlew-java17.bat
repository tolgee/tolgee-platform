@echo off
setlocal

REM Check if JAVA_HOME_17 is set
if defined JAVA_HOME_17 (
    set JAVA_HOME=%JAVA_HOME_17%
    goto execute
)

REM Try to find Java 17 in common installation locations
for %%i in (
    "C:\Program Files\Java\jdk-17*"
    "C:\Program Files\Eclipse Adoptium\jdk-17*"
    "C:\Program Files\Amazon Corretto\jdk17*"
    "C:\Program Files\BellSoft\LibericaJDK-17*"
) do (
    if exist %%i (
        set JAVA_HOME=%%i
        goto execute
    )
)

echo Java 17 not found. Please set JAVA_HOME_17 environment variable to point to a Java 17 installation.
exit /b 1

:execute
echo Using Java 17 from %JAVA_HOME%
call backend\gradlew.bat %*
endlocal 
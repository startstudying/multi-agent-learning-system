@echo off
REM Maven Wrapper for Windows
REM Use as: mvnw.cmd <maven-args>
REM First run will download Maven automatically.

setlocal EnableDelayedExpansion

if not defined JAVA_HOME (
    echo ERROR: JAVA_HOME is not set.
    echo Please install JDK 21 and set JAVA_HOME.
    echo   Example: set JAVA_HOME=C:\Program Files\Java\jdk-21
    echo   Or run: setx JAVA_HOME "C:\Program Files\Java\jdk-21"
    exit /b 1
)

if not exist "!JAVA_HOME!\bin\java.exe" (
    echo ERROR: java.exe not found at !JAVA_HOME!\bin\java.exe
    echo Please check your JAVA_HOME setting.
    exit /b 1
)

set "MAVEN_OPTS=-Xmx1024m %MAVEN_OPTS%"

"!JAVA_HOME!\bin\java.exe" ^
    -classpath "%~dp0.mvn\wrapper\maven-wrapper.jar" ^
    -Dmaven.multiModuleProjectDirectory="%~dp0." ^
    org.apache.maven.wrapper.MavenWrapperMain ^
    %*

@REM ----------------------------------------------------------------------------
@REM Maven Wrapper startup batch script
@REM Descarga Maven automáticamente si no está instalado en el sistema.
@REM Uso: mvnw.cmd clean compile   /   mvnw.cmd javafx:run
@REM ----------------------------------------------------------------------------
@echo off
setlocal

set MAVEN_WRAPPER_JAR="%~dp0.mvn\wrapper\maven-wrapper.jar"
set MAVEN_WRAPPER_PROPERTIES="%~dp0.mvn\wrapper\maven-wrapper.properties"
set DOWNLOAD_URL=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar

for /f "usebackq tokens=1,2 delims==" %%a in (%MAVEN_WRAPPER_PROPERTIES%) do (
    if "%%a"=="distributionUrl" set DISTRIBUTION_URL=%%b
)

set JAVA_HOME_EXE="%JAVA_HOME%\bin\java.exe"
if exist %JAVA_HOME_EXE% goto OkJHome

set JAVA_HOME_EXE="java"
:OkJHome

if exist %MAVEN_WRAPPER_JAR% goto runWrapper

echo Descargando maven-wrapper.jar ...
%JAVA_HOME_EXE% -classpath "" ^
  "-Dmaven.wrapper.properties.path=%MAVEN_WRAPPER_PROPERTIES%" ^
  org.apache.maven.wrapper.MavenWrapperMain %*
goto end

:runWrapper
%JAVA_HOME_EXE% -classpath %MAVEN_WRAPPER_JAR% ^
  "-Dmaven.wrapper.properties.path=%MAVEN_WRAPPER_PROPERTIES%" ^
  org.apache.maven.wrapper.MavenWrapperMain %*

:end
endlocal
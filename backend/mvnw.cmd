@REM ----------------------------------------------------------------------------
@REM Maven Start Up Batch script
@REM ----------------------------------------------------------------------------
@echo off
set ERROR_CODE=0

@REM set HOME = %HOMEPATH% Metavariable
if "%HOME%" == "" (set "HOME=%HOMEDRIVE%%HOMEPATH%")

@REM Set MAVEN_PROJECT_BASEDIR
set MAVEN_PROJECT_BASEDIR=%~dp0

if not "%MAVEN_PROJECT_BASEDIR%" == "" goto OkBaseDir
set MAVEN_PROJECT_BASEDIR=%CD%

:OkBaseDir
set WRAPPER_JAR="%MAVEN_PROJECT_BASEDIR%\.mvn\wrapper\maven-wrapper.jar"
set WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

if exist %WRAPPER_JAR% goto run

echo Could not find %WRAPPER_JAR%, downloading...
powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; (New-Object Net.WebClient).DownloadFile('https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar', '%WRAPPER_JAR%')"

:run
"%JAVA_HOME%\bin\java.exe" -jar %WRAPPER_JAR% %*
if ERRORLEVEL 1 set ERROR_CODE=1
goto end

:end
exit /B %ERROR_CODE%

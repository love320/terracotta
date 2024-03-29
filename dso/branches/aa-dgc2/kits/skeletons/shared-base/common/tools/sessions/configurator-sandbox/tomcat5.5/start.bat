@echo off

rem All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.

rem -------------------------------------
rem - start.bat 908{1,2} [nodso]
rem -------------------------------------

setlocal
cd %~d0%~p0..
set SANDBOX=%CD%
set SANDBOX="%SANDBOX:"=%"

set TC_INSTALL_DIR=%SANDBOX%\..\..\..

if not defined JAVA_HOME set JAVA_HOME="%TC_INSTALL_DIR%\jre"
set JAVA_HOME="%JAVA_HOME:"=%"

if not defined CATALINA_HOME (
   echo CATALINA_HOME must be set to a Tomcat5.5 installation.
   exit /b 1
)
set CATALINA_HOME="%CATALINA_HOME:"=%"

if not exist %CATALINA_HOME% (
   echo CATALINA_HOME %CATALINA_HOME% does not exist.
   exit /b 1
)
set CATALINA_BASE=%SANDBOX%\tomcat5.5\%1

rem --------------------------------------------------------------------
rem - The Configurator passes 'nodso' as the second argument to this
rem - script if you've disabled DSO in its GUI...
rem --------------------------------------------------------------------

if "%2" == "nodso" goto runCatalina

set TC_CONFIG_PATH=%SANDBOX%\tomcat5.5\tc-config.xml
call %TC_INSTALL_DIR%\bin\dso-env.bat -q "%TC_CONFIG%"

if %ERRORLEVEL% NEQ 0 goto end

set OPTS=%TC_JAVA_OPTS% -Dwebserver.log.name=tomcat-%1
set OPTS=%OPTS% -Dcom.sun.management.jmxremote
set OPTS=%OPTS% -Dproject.name=Configurator
set JAVA_OPTS=%OPTS% %JAVA_OPTS%

:runCatalina

cd %SANDBOX%
set CATALINA_HOME=%CATALINA_HOME:"=%
set CATALINA_BASE=%CATALINA_BASE:"=%
set JAVA_HOME=%JAVA_HOME:"=%
call "%CATALINA_HOME%\bin\catalina.bat" run

:end
exit /b %ERRORLEVEL%
endlocal

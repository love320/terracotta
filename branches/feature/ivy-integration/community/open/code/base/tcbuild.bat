@echo off
rem
rem All content copyright (c) 2003-2006 Terracotta, Inc.,
rem except as may otherwise be noted in a separate copyright notice.
rem All rights reserved
rem
IF NOT EXIST build-tc.rb GOTO no_build_tc

set JRUBY_HOME=%~p0..\..\buildsystems\jruby

:has_jruby_home
if "x%JAVA_HOME%"=="x" GOTO has_no_java_home
GOTO has_java_home

:has_no_java_home
SET JAVA_HOME=%TC_JAVA_HOME_15%

:has_java_home
if NOT EXIST "%JAVA_HOME%" GOTO still_has_no_java_home
GOTO located_java_home

:still_has_no_java_home
echo Your JAVA_HOME (possibly located via TC_JAVA_HOME_15), "%JAVA_HOME%", does not exist. You must set this and re-run the script.
GOTO end

:located_java_home
%JRUBY_HOME%\bin\jruby.bat -Ibuildscripts build-tc.rb %*
set TCBUILD_ERR=%ERRORLEVEL%
GOTO end

:no_build_tc
	echo There is no build-tc.rb file in this directory. Please run this script from a directory with a build-tc.rb file.
	GOTO end

:end
echo tcbuild.bat: exit code is %E%
exit /b %TCBUILD_ERR%

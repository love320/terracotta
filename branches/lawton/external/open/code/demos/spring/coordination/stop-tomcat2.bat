@echo off
rem @COPYRIGHT@

setlocal
set topdir=%~d0%~p0..\..

if "x%tc_install_dir%"=="x" set tc_install_dir=%topdir%\..
set catalina_home=%tc_install_dir%\vendors\tomcat5.5

call "%topdir%\libexec\tc-functions.bat" tc_install_dir "%tc_install_dir%" true
if "%exitflag%"=="true" goto end

set catalina_base=tomcat2
set java_home=%tc_java_home%

echo "stopping terracotta for spring: thread coordination sample: tomcat server node 2" 
"%catalina_home%\bin\catalina.bat" stop

:end
endlocal

@echo off
set JAVA_EXE=java
set JAVA_OPTS=-Xmx2024m 

rem In case you need to remotely debug CAAS itself
rem set JAVA_OPTS=%JAVA_OPTS% -Xdebug -Xrunjdwp:transport=dt_socket,address=127.0.0.1:9302,server=y,suspend=y 

%JAVA_EXE% %JAVA_OPTS% -cp ../classes;../caas.jar org.kisst.cordys.caas.main.CaasMain -c ..\config\caas.conf %* 


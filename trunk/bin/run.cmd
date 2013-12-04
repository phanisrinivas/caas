@echo off
set JAVA_EXE=java
set JAVA_OPTS=-Xmx1024m 

rem In case you need to remotely debug CAAS itself
rem set JAVA_OPTS=%JAVA_OPTS% -Xdebug -Xrunjdwp:transport=dt_socket,address=127.0.0.1:9302,server=y,suspend=y 

%JAVA_EXE% %JAVA_OPTS% -cp ../caas.jar;../classes org.kisst.cordys.caas.main.CaasMain -c ..\config\caas.conf -v run ..\scripts\%1.groovy %2 %3 %4 %5 %6 %7 %8 %9


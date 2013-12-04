@echo off
set JAVA_EXE=java
set JAVA_OPTS=-Xmx1024m 

rem In case you need to remotely debug CAAS itself
rem @set JAVA_OPTS=%JAVA_OPTS% -Xdebug -Xrunjdwp:transport=dt_socket,address=127.0.0.1:9302,server=y,suspend=y 

%JAVA_EXE% %JAVA_OPTS% -jar ../caas.jar -c ..\config\caas.conf -v shell %* 


@echo off
set JAVA_EXE=java
set JAVA_OPTS=-Xmx1024m 

rem In case you need to remotely debug CAAS itself
rem set JAVA_OPTS=%JAVA_OPTS% -Xdebug -Xrunjdwp:transport=dt_socket,address=127.0.0.1:9302,server=y,suspend=y 

set GROOVY_SCRIPT=%1

rem Throw the first parameter away and build up a variable with the rest of the parameters.
SHIFT

set REST_OF_PARAMETERS=%1

:loop
SHIFT
if [%1]==[] goto afterloop
set REST_OF_PARAMETERS=%REST_OF_PARAMETERS% %1
goto loop

:afterloop

%JAVA_EXE% %JAVA_OPTS% -cp ../caas.jar;../classes org.kisst.cordys.caas.main.CaasMain -c ..\config\caas.conf -v run ..\scripts\%GROOVY_SCRIPT%.groovy %REST_OF_PARAMETERS%
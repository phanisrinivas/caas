
@set JAVA_EXE=java
@set JAVA_OPTS=-Xmx512m 

rem @set JAVA_OPTS=%JAVA_OPTS% -Xdebug -Xrunjdwp:transport=dt_socket,address=127.0.0.1:9302,server=y,suspend=y 

%JAVA_EXE% %JAVA_OPTS% -jar ../caas.jar %1 %2 %3 %4 %5 %6 %7 %8 %9 


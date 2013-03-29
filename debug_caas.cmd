@set JAVA_EXE=java
@set JAVA_OPTS=-Xmx1024m 
@set JAVA_OPTS=%JAVA_OPTS% -Xdebug -Xrunjdwp:transport=dt_socket,address=127.0.0.1:9302,server=y,suspend=y 

%JAVA_EXE% %JAVA_OPTS% -cp "build/classes;lib/commons-cli-1.2.jar;lib/commons-codec-1.2.jar;lib/commons-httpclient-3.1.jar;lib/commons-logging-1.0.4.jar;lib/groovy-all-2.0.6.jar;lib/jansi-1.6.jar;lib/jaxen-1.1.4.jar;lib/jdom-2.0.4.jar;lib/jline-1.0.jar;lib/log4j-1.2.13.jar" org.kisst.cordys.caas.main.CaasMain %*


#! /bin/sh
export JAVA_EXE=java
export JAVA_OPTS=-Xmx2024m 

## In case you need to remotely debug CAAS itself
# export JAVA_OPTS=$JAVA_OPTS -Xdebug -Xrunjdwp:transport=dt_socket,address=127.0.0.1:9302,server=y,suspend=y 

$JAVA_EXE $JAVA_OPTS -jar ../caas.jar -c ../config/caas.conf $* 


#!/bin/sh
export JAVA_EXE=java
export JAVA_OPTS=-Xmx1024m 

## In case you need to remotely debug CAAS itself
# export JAVA_OPTS=$JAVA_OPTS -Xdebug -Xrunjdwp:transport=dt_socket,address=127.0.0.1:9302,server=y,suspend=y 

# Find the name of the Groovy script to run
export GROOVY_SCRIPT=$1
shift

# Save the rest of the parameters
export REST_OF_PARAMETERS=

while test ${#} -gt 0
do
  export REST_OF_PARAMETERS=$REST_OF_PARAMETERS $1
  shift
done

$JAVA_EXE $JAVA_OPTS -cp ../classes;../caas.jar org.kisst.cordys.caas.main.CaasMain -c ../config/caas.conf -v run ../scripts/$GROOVY_SCRIPT.groovy $REST_OF_PARAMETERS
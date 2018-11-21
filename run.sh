#!/bin/sh
java -jar server/target/web-frontend-1.0-SNAPSHOT-jar-with-dependencies.jar -DPROPERTY_FILE=./ADAM.properties &
echo $! > ../adamweb.pid

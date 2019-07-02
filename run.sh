#!/bin/sh
java -jar server/target/web-frontend-1.0-SNAPSHOT-jar-with-dependencies.jar -DPROPERTY_FILE=./ADAM.properties 2>&1 | tee adamweb-log-`date +%Y-%m-%d-%Hh%Mm%Ss`.txt &
echo $! > ../adamweb.pid

#!/bin/sh
java -DPROPERTY_FILE=./ADAM.properties -jar server/target/web-frontend-1.0-SNAPSHOT-jar-with-dependencies.jar 2>&1 | tee adamweb-log-`date +%Y-%m-%d-%Hh%Mm%Ss`.txt &

# Save the pid of the server 
# echo $! > ../adamweb.pid  # Doesnt work within our pipeline, returns the PID of 'tee'
jobs -p > ../adamweb.pid # returns the PID of the first command in the pipeline, i.e. java

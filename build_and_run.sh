#!/bin/sh
cd server/ && mvn clean package && java -jar target/web-frontend-1.0-SNAPSHOT-jar-with-dependencies.jar

#!/bin/bash
# @author Manuel Gieseking

# read the current version number out of the pom.xml
ver=$(grep -zoP "<artifactId>adam-core</artifactId>[[:space:]]*<version>\K[0-9]+\.[0-9]+(?=</version>)" ../server/pom.xml | tr '\0' '\n')
echo "> Found version ${ver}."
echo "> Create the adam_core.jar" 
make deploy_backend 
echo "> Integrate version ${ver} of adam_core.jar"
mvn org.apache.maven.plugins:maven-install-plugin:2.3.1:install-file -Dfile=webinterface-backend/adam_core.jar -DgroupId=uniolunisaar.adam -DartifactId=adam-core -Dversion=${ver} -Dpackaging=jar -DlocalRepositoryPath=../server/adam-core
echo "> Clean up"
make clean_backend

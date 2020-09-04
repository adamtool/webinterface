#!/bin/bash
# @author Manuel Gieseking
if [[ -z ${version} ]]; then
	echo 'You have to give a version number with version="<version>".'
else
    echo "> Create the adam_core.jar" 
    make deploy_backend 
    echo "> Publish this version of adam_core.jar"
    mvn org.apache.maven.plugins:maven-install-plugin:2.3.1:install-file -Dfile=webinterface-backend/adam_core.jar -DgroupId=uniolunisaar.adam -DartifactId=adam-core -Dversion=${version} -Dpackaging=jar -DlocalRepositoryPath=server/adam-core
    make clean_backend
fi

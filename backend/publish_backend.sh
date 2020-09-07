#!/bin/bash
# @author Manuel Gieseking
if [[ -z ${ver} ]]; then
	echo 'You have to give a version number with ver="<version>".'
else
    echo "> Create the adam_core.jar" 
    make deploy_backend 
    echo "> Publish this version of adam_core.jar"
    mvn org.apache.maven.plugins:maven-install-plugin:2.3.1:install-file -Dfile=webinterface-backend/adam_core.jar -DgroupId=uniolunisaar.adam -DartifactId=adam-core -Dversion=${ver} -Dpackaging=jar -DlocalRepositoryPath=../server/adam-core
    echo "> Update the pom.xml"
    # this command is quite dangerous and very dependent on the structure of the pom.xml
    # searches for the line containing "<artifactId>adam-core</artifactId>"
    # goes to the next line (n) and replaces the version string there
    sed -i "/<artifactId>adam-core<\/artifactId>/{n;s/<version>\([0-9]\+\.[0-9]\+\)<\/version>/<version>${ver}<\/version>/}" ../server/pom.xml 
    echo "> Clean up"
    make clean_backend
fi

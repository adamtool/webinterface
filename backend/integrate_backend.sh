#!/bin/bash
# @author Manuel Gieseking
function printColored {
    col="\e[1;36m"
    if [[ ! -z $2 ]]; then
        col=$2
    fi
    echo -e "$col$1\e[0m"
}

function printError {
    printColor $1 "\e[1;31m"
}

# read the current version number out of the pom.xml
ver=$(grep -zoP "<artifactId>adam-core</artifactId>[[:space:]]*<version>\K[0-9]+\.[0-9]+(?=</version>)" ../server/pom.xml | tr '\0' '\n')
printColored "> Found version ${ver}." "\e[1;32m"
printColored "> Create the adam_core.jar" 
make deploy_backend 
printColored "> Integrate version ${ver} of adam_core.jar"
mvn org.apache.maven.plugins:maven-install-plugin:2.3.1:install-file -Dfile=webinterface-backend/adam_core.jar -DgroupId=uniolunisaar.adam -DartifactId=adam-core -Dversion=${ver} -Dpackaging=jar -DlocalRepositoryPath=../server/adam-core
printColored "> Clean up"
make clean_backend

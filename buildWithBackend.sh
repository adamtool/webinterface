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

pref="./server/adam-core/uniolunisaar/adam/adam-core/"
ver=$(grep -zoP "<artifactId>adam-core</artifactId>[[:space:]]*<version>\K[0-9]+\.[0-9]+(?=</version>)" ./server/pom.xml | tr '\0' '\n')
folder=${pref}${ver}

if [ ! -d "${folder}" ]; then
    printError "> The backend is not compiled for version: ${ver}."
    cd backend && make integrate_backend && cd ..
fi
printColored "> update the examples."
cd backend && make update_examples clean=true && cd ..
printColored "> build the server."
./build.sh

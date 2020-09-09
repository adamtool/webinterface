#!/bin/bash
# @author Manuel Gieseking
pref="./server/adam-core/uniolunisaar/adam/adam-core/"
ver=$(grep -zoP "<artifactId>adam-core</artifactId>[[:space:]]*<version>\K[0-9]+\.[0-9]+(?=</version>)" ./server/pom.xml | tr '\0' '\n')
folder=${pref}${ver}

if [ ! -d "${folder}" ]; then
    echo "> The backend is not compiled for version: ${ver}."
    cd backend && make integrate_backend && make update_examples
    cd ..
fi
echo "> build and run the server."
./build_and_run.sh

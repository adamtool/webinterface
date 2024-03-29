#!/bin/bash
# @author Manuel Gieseking

# import the coloring functions for the texts
source ./backend/echoColoredTexts.sh

pref="./server/adam-core/uniolunisaar/adam/adam-core/"
ver=$(grep -zoP "<artifactId>adam-core</artifactId>[[:space:]]*<version>\K[0-9]+\.[0-9]+(?=</version>)" ./server/pom.xml | tr '\0' '\n')
folder=${pref}${ver}

if [ ! -d "${folder}" ]; then
    printColored "> The backend is not compiled for version: ${ver}." $yellow
    cd backend && make integrate_backend && cd ..
else 
    printColored "> The backend is already compiled for version: ${ver}." $green
fi
printColored "> update the examples."
cd backend && make update_examples clean=true && cd ..
printColored "> build the server."
./build.sh

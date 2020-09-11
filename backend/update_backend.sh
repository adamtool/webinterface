#!/bin/bash
# @author Manuel Gieseking

IFS=',' read -r -a dep_folders <<< "$1"

function printColored {
    echo -e "\e[1;36m$1\e[0m"
}

function printError {
    echo -e "\e[1;31m$1\e[0m"
}

printColored "> Add the changes of any changed dependency."
for dep in "${dep_folders[@]}"	# all dependencies
    do	
	    echo "%%%%%%%%%%%%%%%% DEPENDENCY: $dep"
	    if [ ! -f "$dep/.git" ]; then
            printError "The dependency is missing. Please execute 'git submodule update --init "$dep"' first."
        else           
            git add $dep
        fi
done

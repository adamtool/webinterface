#!/bin/bash
# @author Manuel Gieseking

IFS=',' read -r -a dep_folders <<< "$1"

echo "> Add the changes of any changed dependency."
for dep in "${dep_folders[@]}"	# all dependencies
    do	
	    echo "%%%%%%%%%%%%%%%% DEPENDENCY: $dep"
	    if [ ! -f "$dep/.git" ]; then
            echo "The dependency is missing. Please execute 'git submodule update --init "$dep"' first."
        else           
            git add $dep
        fi
done

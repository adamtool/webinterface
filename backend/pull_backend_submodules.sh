#!/bin/bash
# @author Manuel Gieseking

IFS=',' read -r -a dep_folders <<< "$1"

echo "> Pull all dependencies:"
for dep in "${dep_folders[@]}"	# all dependencies
    do	
	    echo "%%%%%%%%%%%%%%%% DEPENDENCY: $dep"
	    if [ ! -f "$dep/.git" ]; then
            echo "The dependency is missing. Please execute 'git submodule update --init "$dep"' first."
        else
            cd $dep                
            git pull
            cd ..
        fi
done

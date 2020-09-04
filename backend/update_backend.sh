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
echo "> Commit the update to a new version of the submodules."
git commit -m "Updated the backend to the current version of the submodules."
echo "> Push the changes."
git push
echo "> Updated the backend to the current version of the submodules and pushed it."

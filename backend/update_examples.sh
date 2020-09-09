#!/bin/bash
# @author Manuel Gieseking
MC_FOLDER=../client/src/assets/apt-examples-mc/
SYNT_FOLDER=../client/src/assets/apt-examples-synthesis/
EXAMPLES_MC_FOLDER=./examples/modelchecking/
EXAMPLES_SYNT_FOLDER=./examples/synthesis/forallsafety/

###### Model checking
echo "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% MODEL CHECKING"
if [[ ! -z ${clean} ]]; then
    echo "> MC: Remove all current examples ..."
    rm -r $MC_FOLDER
    mkdir $MC_FOLDER
fi
echo "> MC: Copy examples with coordinates..."
for file in $(find $EXAMPLES_MC_FOLDER -name '*.apt'); do   # all .apt files recursively
    if grep -q "xCoord" "$file"; then
        # tinker the new location
        new_file=${file/"$EXAMPLES_MC_FOLDER"/"$MC_FOLDER"}
        new_folder=${new_file%/*}"/"
        mkdir -p $new_folder   # create the folder 
        echo $file" -> "$new_file
        cp $file $new_file  # copy the file
    fi
done

###### Synthesis
echo -e "\n%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% SYNTHESIS"
if [[ ! -z ${clean} ]]; then
    echo "> SYNT: Remove all current examples ..."
    rm -r $SYNT_FOLDER
    mkdir $SYNT_FOLDER
fi
echo "> SYNT: Copy examples with coordinates..."
for file in $(find $EXAMPLES_SYNT_FOLDER -name '*.apt'); do   # all .apt files recursively
    if grep -q "xCoord" "$file"; then
        # tinker the new location
        new_file=${file/"$EXAMPLES_SYNT_FOLDER"/"$SYNT_FOLDER"}
        new_folder=${new_file%/*}"/"
        mkdir -p $new_folder   # create the folder 
        echo $file" -> "$new_file
        cp $file $new_file  # copy the file
    fi
done

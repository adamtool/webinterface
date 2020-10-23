#!/bin/bash
# @author Manuel Gieseking

# import the coloring functions for the texts
source ./echoColoredTexts.sh

# some folders
MC_FOLDER=../client/src/assets/apt-examples-mc/
SYNT_FOLDER=../client/src/assets/apt-examples-synthesis/
EXAMPLES_MC_FOLDER=./examples/modelchecking/ltl/
EXAMPLES_SYNT_FOLDER=./examples/synthesis/forallsafety/
SYNT_EXCLUDING_FOLDERS="-not -path */boundedunfolding/* -not -path */ndet/*"
SYNT_EXCLUDING_FILES='-not -path */forallsafety/burglar/burglar.apt -not -path */forallsafety/container/container.apt -not -path */forallsafety/nm/sendingprotocol.apt'
#SYNT_EXCLUDING_FILES=''
MC_EXCLUDING_FOLDERS=''
MC_EXCLUDING_FILES=''


###### Model checking
printColored "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% MODEL CHECKING" $blue
if [[ ! -z ${clean} ]]; then
    printColored "> MC: Remove all current examples ..."
    rm -r $MC_FOLDER
    mkdir $MC_FOLDER
fi
printColored "> MC: Copy examples with coordinates..."
for file in $(find $EXAMPLES_MC_FOLDER -name '*.apt'  $MC_EXCLUDING_FOLDERS $MC_EXCLUDING_FILES); do   # all .apt files recursively
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
printColored "\n%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% SYNTHESIS" $blue
if [[ ! -z ${clean} ]]; then
    printColored "> SYNT: Remove all current examples ..."
    rm -r $SYNT_FOLDER
    mkdir $SYNT_FOLDER
fi
printColored "> SYNT: Copy examples with coordinates..."
for file in $(find $EXAMPLES_SYNT_FOLDER -name '*.apt' $SYNT_EXCLUDING_FOLDERS $SYNT_EXCLUDING_FILES); do   # all .apt files recursively
    if grep -q "xCoord" "$file"; then
        # tinker the new location
        new_file=${file/"$EXAMPLES_SYNT_FOLDER"/"$SYNT_FOLDER"}
        new_folder=${new_file%/*}"/"
        mkdir -p $new_folder   # create the folder 
        echo $file" -> "$new_file
        cp $file $new_file  # copy the file
    fi
done

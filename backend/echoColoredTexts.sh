#!/bin/bash
# @author Manuel Gieseking
## text colors
reset=0
black=30
red=31
green=32
yellow=33
blue=34
magenta=35
cyan=36
white=37
## background colors
bg_black=40
bg_red=41
bg_green=42
bg_yellow=43
bg_blue=44
bg_magenta=45
bg_cyan=46
bg_white=47

printColored () {
    col=$cyan
    if [[ ! -z $2 ]]; then
        col=$2
    fi
    echo -e "\e[1;"$col"m"$1"\e[0m"
}

printError () {
    printColored "ERROR: $1" $red
}


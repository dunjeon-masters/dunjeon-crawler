#!/bin/bash

function my_clear {
    for i in `seq 1 42`;
    do
        for j in `seq 1 80`;
        do
            printf "="
        done
        printf "\n"
    done
}

clear
my_clear
lein clean
lein run 2> >(grep -P "(\tat rouje)|Caused|Exception")

#!/bin/bash

clear
lein clean
lein run 2> >(grep -P "(\tat rouje)|Caused|Exception")

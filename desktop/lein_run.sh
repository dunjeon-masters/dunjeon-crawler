#!/bin/bash

clear
lein run 2> >(grep -P "(\tat rouje)|Caused|Exception")

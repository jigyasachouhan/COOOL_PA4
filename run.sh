#!/usr/bin/bash
javac ./testcases/$1/*.java
java -cp .:soot-4.6.0-jar-with-dependencies.jar PA4 $1
#!/usr/bin/bash
# get the time stamp for unique output file names
timestamp=$(date +%s)

javac ./testcases/$1/*.java
java -cp .:soot-4.6.0-jar-with-dependencies.jar PA4 $1 false $timestamp
java -cp .:soot-4.6.0-jar-with-dependencies.jar PA4 $1 true $timestamp
# mv OriginalOutput* OriginalOutput
# mv sootOutput* sootOutput

# folder name is of the form sootOutput_$1_$timestamp and OriginalOutput_$1_$timestamp
echo "Compiling and running original classes"
javac -cp .:OriginalOutput_$1_$timestamp yash.java
java -Xint -cp .:OriginalOutput_$1_$timestamp yash > tmp.txt

echo "Running with sootOutput classes"
javac -cp .:sootOutput_$1_$timestamp yash.java
java -Xint -cp .:sootOutput_$1_$timestamp yash > tmp.txt
rm tmp.txt

# Check the difference in outputs of Test.java when run with original classes and sootOutput classes
echo "Comparing outputs"
cd OriginalOutput_$1_$timestamp
java Test > ../original_output.txt
cd ../sootOutput_$1_$timestamp
java Test > ../soot_output.txt
cd ..
diff original_output.txt soot_output.txt


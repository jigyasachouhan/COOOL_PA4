echo "Compiling and running original classes"
javac -cp .:OriginalOutput yash.java
java -Xint -cp .:OriginalOutput yash

echo "Running with sootOutput classes"
javac -cp .:sootOutput yash.java
java -Xint -cp .:sootOutput yash


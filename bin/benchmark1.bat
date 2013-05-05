set output_file=D:\out.txt
del %output_file% 

set test_dir=D:/TestData

java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% add %test_dir%"/JavaApplication1.java" japp
java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% add %test_dir%"/JavaApplication2.java" japp
java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% add %test_dir%/big_files
java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% add %test_dir%/big_files/file0 big_files/file1
java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% add %test_dir%/big_files/file1 big_files/file0
java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% add %test_dir%/small_files
java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% add %test_dir%"/[NMAI062] Algebra I"
java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% add %test_dir%"/[DBI026] dbapl"
java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% add %test_dir%"/WebApplication5"
java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% add %test_dir%/documents
java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% get big_files/file0 ../data/fileA
java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% get_zip big_files/file0 ../data/fileB
java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% get small_files ../data
java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% get "[NMAI062] Algebra I" ../data
java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% delete big_files/file1 0

del /f /s /q "..\data\[NMAI062] Algebra I"
del /f /s /q "..\data\small_files"
del /f /s /q "..\data\fileA"
del /f /s /q "..\data\fileB"

pause
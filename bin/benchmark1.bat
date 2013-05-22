:: Output file, to which the performance testing results will be saved.
set output_file=D:\out.txt

:: Directory with the test data described in the thesis.
set test_dir=D:/TestData

:: Number of iterations of the test.
set test_count=50


del %output_file%

for /L %%j in (1,1,%test_count%) do (
	echo ITERATION %%j OF %test_count%
	server_test.vbs
	wait.vbs

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
	java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% endline

	wait.vbs
	taskkill /F /IM java.exe	
)

pause
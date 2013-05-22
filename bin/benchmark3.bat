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

	for /L %%i in (0,1,14) do (
		java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% add %test_dir%/similar/file%%i similar
	)
	java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% delete japp 1
	java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% endline

	wait.vbs
	taskkill /F /IM java.exe	
)

pause
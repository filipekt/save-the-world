:: Output file, to which the performance testing results will be saved.
set output_file=D:\out.txt

:: Directory with the test data described in the thesis.
set test_dir=D:/TestData

:: Number of iterations of the test.
set test_count=50

del %output_file%

:: Size of the file to be added.
set filesize=5

:: Size of the database before adding the file.
set previous_size_10m=1
set /a upper=(%previous_size_10m% - 1)

for /L %%j in (1,1,%test_count%) do (
	echo ITERATION %%j OF %test_count%
	server_test.vbs
	wait.vbs	
	for /L %%k in (0,1,%upper%) do (
		java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% add %test_dir%"/Files10M/file_10M_%%k"
	)
	java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% add %test_dir%"/file_size_%filesize%MB" rnd

	java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% endline
	wait.vbs
	taskkill /F /IM java.exe	
)

pause
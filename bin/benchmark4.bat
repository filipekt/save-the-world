set output_file=D:\out.txt
set test_dir=D:/TestData
set test_count=50
del %output_file%
set filesize=20

for /L %%j in (1,1,%test_count%) do (
	echo ITERATION %%j OF %test_count%
	server_test.vbs
	wait.vbs
	java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% add %test_dir%"/Files10M/file_10M_0"

	java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% add %test_dir%"/file_size_%filesize%MB" rnd

	java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% endline
	wait.vbs
	taskkill /F /IM java.exe	
)

pause
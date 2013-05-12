set output_file=D:\out.txt
set test_dir=D:/TestData
set test_count=1
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
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

	java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% add %test_dir%"/lipsum.txt" lipsum
	java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% add %test_dir%"/lipsum2.txt" lipsum
	java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% add %test_dir%"/lipsum.txt" lipsum
	java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% add %test_dir%"/lipsum2.txt" lipsum
	java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% add %test_dir%"/blank" blank
	java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% add %test_dir%"/blank2" blank
	java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% add %test_dir%"/blank" blank
	java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% add %test_dir%"/blank2" blank
	java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% get lipsum ../data/lipsumA 0
	java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% get lipsum ../data/lipsumB 1
	java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% get lipsum ../data/lipsumC 2
	java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% get blank ../data/blankA 0
	java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% get blank ../data/blankB 1
	java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% get blank ../data/blankA 2
	java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% delete blank 1
	java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% delete blank 0
	java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% delete lipsum 2
	java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% delete lipsum 0

	java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% endline
	wait.vbs
	taskkill /F /IM java.exe	
)

pause
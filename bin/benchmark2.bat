set output_file=D:\out.txt
del %output_file% 

set test_dir=D:/TestData

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

del /f /s /q "..\data\lipsumA"
del /f /s /q "..\data\lipsumB"
del /f /s /q "..\data\lipsumC"
del /f /s /q "..\data\blankA"
del /f /s /q "..\data\blankB"
del /f /s /q "..\data\blankC"

pause
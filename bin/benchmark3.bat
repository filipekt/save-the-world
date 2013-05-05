set output_file=D:\out.txt
del %output_file% 

set test_dir=D:/TestData


java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% add %test_dir%"/JavaApplication1.java" japp
java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% add %test_dir%"/JavaApplication2.java" japp
java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% add %test_dir%/similar/file0 similar
java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% add %test_dir%/similar/file1 similar
java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% add %test_dir%/similar/file2 similar
java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% add %test_dir%/similar/file3 similar
java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% add %test_dir%/similar/file4 similar
java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% add %test_dir%/similar/file5 similar
java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% add %test_dir%/similar/file6 similar
java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% add %test_dir%/similar/file7 similar
java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% add %test_dir%/similar/file8 similar
java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% add %test_dir%/similar/file9 similar
java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% add %test_dir%/similar/file10 similar
java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% add %test_dir%/similar/file11 similar
java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% add %test_dir%/similar/file12 similar
java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% add %test_dir%/similar/file13 similar
java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% add %test_dir%/similar/file14 similar
java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% delete japp 1
pause
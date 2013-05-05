:: The IP address of the server 
set COMP=127.0.0.1

:: Number of the port used by the server
set PORT=6666

:: Time interval (in seconds) between two executions of the scheduled code.
set TIME=5

:: The path to a file which contains the scheduled user commands.
set INPUT_FILE=./scheduler_commands

:: The maximum number of executions of the scheduled code.
:: Zero value means there is no limit set.
set COUNT=0

:: Path to the jar containing the application
set PATH_TO_JAR=../target/save-the-world.jar

java -server -cp %PATH_TO_JAR% cz.filipekt.Scheduler -c %COMP% -p %PORT% -time %TIME% -input %INPUT_FILE% -count %COUNT%
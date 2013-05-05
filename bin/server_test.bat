:: Port number, where the server will listen.
set PORT=6666

:: Directory, where all the dtabase file will be stored.
set HOME_DIR="../data"

:: Total space (in bytes) reserved for the application.
:: If set to 0, no limit is set.
set RESERVED_SPACE=0

:: Path to the jar containing the application
set PATH_TO_JAR=../target/save-the-world.jar

del /f /s /q ..\data

java -server -XX:CompileThreshold=1 -cp %PATH_TO_JAR% cz.filipekt.Server -home %HOME_DIR% -p %PORT% -space %RESERVED_SPACE% -expensive 128 -blocksize 8192
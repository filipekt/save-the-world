:: Address of the server
set COMP=127.0.0.1

:: Port used on the server
set PORT=6666

:: Possible values of locale: EN , CZ
set LOCALE=EN

:: Path to the jar containing the application
set PATH_TO_JAR=../target/save-the-world.jar

java -cp %PATH_TO_JAR% cz.filipekt.Client -c %COMP% -p %PORT% -locale %LOCALE% -interactive
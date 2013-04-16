:: Address of the server
set COMP=127.0.0.1

:: Port used on the server
set PORT=6666

:: Possible values of locale: EN , CZ
set LOCALE=EN

:: If no value is set, the client expects a batch job, so no control output is given.
set INTERACTIVE=-interactive

java -cp save-the-world.jar cz.filipekt.Client -c %COMP% -p %PORT% -locale %LOCALE% %INTERACTIVE%
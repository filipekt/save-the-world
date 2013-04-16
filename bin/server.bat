:: Port number, where the server will listen.
set PORT=6666

:: Directory, where all the dtabase file will be stored.
set HOME_DIR="../stw_db"

:: Total space (in bytes) reserved for the application.
:: If set to 0, no limit is set.
set RESERVED_SPACE=0

java -cp save-the-world.jar cz.filipekt.Server -home %HOME_DIR% -p %PORT% -space %RESERVED_SPACE%
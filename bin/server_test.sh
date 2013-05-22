# Port number, where the server will listen.
PORT=6666

# Directory, where all the dtabase file will be stored.
HOME_DIR="../data"

# Total space (in bytes) reserved for the application.
# If set to 0, no limit is set.
RESERVED_SPACE=0

# Path to the jar containing the application
PATH_TO_JAR="../target/save-the-world.jar"

rm -rf ../data

java -server -XX:CompileThreshold=1 -cp $PATH_TO_JAR cz.filipekt.Server -home $HOME_DIR -p $PORT -space $RESERVED_SPACE -expensive 128 -blocksize 65536
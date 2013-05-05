# Port number, where the server will listen.
port=6666

# Directory, where all the dtabase file will be stored.
home="../data"

# Total space (in bytes) reserved for the application.
# If set to 0, no limit is set.
reserved_space=0

# Path to the jar containing the application
path_to_jar="../target/save-the-world.jar"

java -server -cp $path_to_jar cz.filipekt.Server -home "$home" -p $port -space $reserved_space
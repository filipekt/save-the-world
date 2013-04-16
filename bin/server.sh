#!/bin/sh

# Port number, where the server will listen.
port=6666

# Directory, where all the dtabase file will be stored.
home="../stw_db"

# Total space (in bytes) reserved for the application.
# If set to 0, no limit is set.
reserved_space=0

java -cp save-the-world.jar cz.filipekt.Server -home "$home" -p $port -space $reserved_space
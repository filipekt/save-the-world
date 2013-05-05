# Address of the server
comp=127.0.0.1

# Port used on the server
port=6666

# Possible values of locale: EN , CZ
locale=EN

# Path to the jar containing the application
path_to_jar="../target/save-the-world.jar"

java -cp $path_to_jar cz.filipekt.Client -c "$comp" -p $port -locale $locale -interactive
# Address of the server
comp=127.0.0.1

# Port used on the server
port=6666

# Possible values of locale: EN , CZ
locale=EN

# If no value is set, the client expects a batch job, so no control output is given.
interactive=-interactive

java -cp save-the-world.jar cz.filipekt.Client -c "$comp" -p $port -locale $locale $interactive
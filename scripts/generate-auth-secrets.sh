#!/bin/sh

mkdir -p local
script="local/auth.env"

echo -n '' > $script

echo -n 'AUTH_SECRET=' >> $script
head --bytes 64 /dev/urandom | base64 -w 0 >> $script
echo '' >> $script

echo -n 'AUTH_PEPPER=' >> $script
head --bytes 64 /dev/urandom | base64 -w 0 >> $script
echo '' >> $script

echo "Environment script has been written to $(pwd)/$script"

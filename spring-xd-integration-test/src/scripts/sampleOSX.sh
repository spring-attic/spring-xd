#!/bin/sh
#provides a quick tcp server for the Mac OSX OS that returns the contents of the
# file index.html upon request
while true; do nc -l -c 8782 < index.html; done

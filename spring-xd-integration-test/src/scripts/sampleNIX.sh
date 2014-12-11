#!/bin/sh
# provides a quick tcp server for the *nix OS that returns the contents of the
# file index.html upon request
while true; do nc -l -C 8782 < index.html; done

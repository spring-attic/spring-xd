#!/bin/bash

BASE_DIR=.

echo ""
echo "Starting Protractor..."
echo $BASE_DIR
echo "-------------------------------------------------------------------"

protractor $1 test/protractor.conf.js

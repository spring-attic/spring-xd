#!/bin/bash

BASE_DIR=.

echo ""
echo "Starting Protractor..."
echo $BASE_DIR
echo "-------------------------------------------------------------------"

protractor test/protractor.conf.js

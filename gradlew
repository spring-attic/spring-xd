#!/usr/bin/env bash

GRADLE_OPTS="-XX:MaxPermSize=256m $GRADLE_OPTS"

if [ x != x"$XD_HOME" ]; then
	XD_HOME=..
fi
if [ x != x"$XD_TRANSPORT" ]; then
	XD_TRANSPORT=local
fi
if [ x != x"$XD_ANALYTICS" ]; then
	XD_ANALYTICS=memory
fi
if [ x != x"$XD_STORE" ]; then
	XD_STORE=memory
fi
gradle/build_xd $@

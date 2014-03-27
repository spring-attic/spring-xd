#!/usr/bin/env bash

GRADLE_OPTS="-Xms128m -Xmx512m -XX:MaxPermSize=512m $GRADLE_OPTS"

if [ x != x"$XD_HOME" ]; then
	unset XD_HOME
fi
if [ x != x"$XD_TRANSPORT" ]; then
	unset XD_TRANSPORT
fi
if [ x != x"$XD_ANALYTICS" ]; then
	unset XD_ANALYTICS
fi
if [ x != x"$XD_STORE" ]; then
	unset XD_STORE
fi
gradle/build_xd $@

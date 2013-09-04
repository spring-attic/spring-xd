#!/usr/bin/env bash
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
./build_xd $@

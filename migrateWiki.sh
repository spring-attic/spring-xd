#!/bin/sh
rm -fR /tmp/spring-xd.wiki

cd /tmp
git clone https://github.com/spring-projects/spring-xd.wiki.git /tmp/spring-xd.wiki
rm /tmp/spring-xd.wiki/guide/_*.asciidoc
rm /tmp/spring-xd.wiki/guide/*.adoc
rm /tmp/spring-xd.wiki/guide/*.xml
find /tmp/spring-xd.wiki/guide -name '*.asciidoc' -exec cp 302.asciidoc {} \;


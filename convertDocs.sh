#!/bin/sh

git clone https://github.com/spring-projects/spring-xd.wiki.git docs-temp
mkdir -p src/docs/asciidoc
cp -r docs-temp/guide/*  src/docs/asciidoc
cp -r docs-temp/images/* src/docs/asciidoc/images


# FullGuide -> index
mv src/docs/asciidoc/FullGuide.adoc src/docs/asciidoc/index.asciidoc
mv src/docs/asciidoc/FullGuide-docinfo.xml src/docs/asciidoc/index-docinfo.xml

# Get rid of GitHub wiki specific 'pages'
rm src/docs/asciidoc/_*


groovy -p -i -e '( line =~ /link:(?!http)(.*?\])/ ).replaceAll("xref:\$1")' `find src/docs/asciidoc/*.asciidoc`

rm -fR docs-temp
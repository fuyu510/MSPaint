#!/bin/sh
# Builds publish/MSPaint.jar: the sources, the resources and the Pumpernickel library in one runnable jar.
set -e
cd "$(dirname "$0")"
OUT=out/build
rm -rf "$OUT"
mkdir -p "$OUT/classes"
find src -name '*.java' > "$OUT/sources.txt"
javac -nowarn -encoding UTF-8 --release 11 -cp lib/Pumpernickel.jar -d "$OUT/classes" @"$OUT/sources.txt"
(cd "$OUT/classes" && jar xf ../../../lib/Pumpernickel.jar && rm -rf META-INF)
cp -r res/* "$OUT/classes/"
jar --create --file publish/MSPaint.jar --manifest src/META-INF/MANIFEST.MF -C "$OUT/classes" .
echo "built publish/MSPaint.jar"

#!/bin/bash -x
mkdir -p lib
cp -rf  "${1:-pzserver}/natives" lib
# relative paths are preserved in the zip file, so we should be at the root of the
# java dir to create this jar file!
CURRENT_DIR="$PWD"
cd "${1:-pzserver}/java"
zip -r "$CURRENT_DIR/lib/pz.jar" **
cp *.jar "$CURRENT_DIR/lib"

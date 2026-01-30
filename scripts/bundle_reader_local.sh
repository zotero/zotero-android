#!/bin/bash

set -eo pipefail

realpath() {
    [[ $1 = /* ]] && echo "$1" || echo "$PWD/${1#./}"
}

SCRIPT_PATH=`realpath "$0"`
SCRIPT_DIR=`dirname "$SCRIPT_PATH"`
SUBMODULE_DIR="$SCRIPT_DIR/../reader"
DESTINATION_DIR="$SCRIPT_DIR/../app/src/main/assets/reader"
HASH_FILE="$DESTINATION_DIR/reader_hash.txt"

CURRENT_HASH=$RANDOM

READER_TO_ZIP_FOLDER="$SUBMODULE_DIR/build/android"

cd "$SUBMODULE_DIR"
npm run build:android

mkdir -p "$DESTINATION_DIR"

cd "$READER_TO_ZIP_FOLDER"
zip -r "$DESTINATION_DIR/reader.zip" "."

echo "$CURRENT_HASH" > "$HASH_FILE"
echo "Build $BUILD_SOURCE_DIR installed at $DESTINATION_DIR from hash $CURRENT_HASH"

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

# Check if the reader submodule is initialized
if ! git -C "$SCRIPT_DIR" submodule status "$SUBMODULE_DIR" | grep -qv '^-'; then
    echo "Error: The reader submodule is not initialized. Run:"
    echo "    git submodule update --init --recursive reader"
    exit 1
fi


CURRENT_HASH=`git ls-tree --object-only HEAD "$SUBMODULE_DIR"`
DOWNLOAD_URL="https://zotero-download.s3.amazonaws.com/ci/reader/${CURRENT_HASH}.zip"

BUILD_SOURCE_DIR="android"

READER_TO_ZIP_FOLDER="$SCRIPT_DIR/../readerToZip"

if [ -d "$DESTINATION_DIR" ]; then
    if [ -f "$HASH_FILE" ]; then
        CACHED_HASH=`cat "$HASH_FILE"`
    else
        CACHED_HASH=0
    fi

    if [ "$CACHED_HASH" == "$CURRENT_HASH" ]; then
        echo "Build already up to date."
        exit
    else
        rm -rf "$DESTINATION_DIR"
    fi
fi

TMP_DIR=$(mktemp -d)
echo "Created temp dir: $TMP_DIR"

echo "Downloading build from: $DOWNLOAD_URL"
curl -L "$DOWNLOAD_URL" -o "$TMP_DIR/build.zip"

echo "Unzipping..."
unzip -q "$TMP_DIR/build.zip" -d "$TMP_DIR/build"

if [ ! -d "$TMP_DIR/build/$BUILD_SOURCE_DIR" ]; then
    echo "Error: $BUILD_SOURCE_DIR build not found in the archive."
    exit 1
fi

mkdir -p "$READER_TO_ZIP_FOLDER"
mkdir -p "$DESTINATION_DIR"

shopt -s dotglob
cp -r "$TMP_DIR/build/$BUILD_SOURCE_DIR/"* "$READER_TO_ZIP_FOLDER"
shopt -u dotglob

cd "$READER_TO_ZIP_FOLDER"
zip -r "$DESTINATION_DIR/reader.zip" "."


echo "$CURRENT_HASH" > "$HASH_FILE"
echo "Build $BUILD_SOURCE_DIR installed at $DESTINATION_DIR from hash $CURRENT_HASH"

rm -rf "$TMP_DIR"
rm -rf "$READER_TO_ZIP_FOLDER"
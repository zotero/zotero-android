#!/bin/bash

set -eo pipefail

realpath() {
    [[ $1 = /* ]] && echo "$1" || echo "$PWD/${1#./}"
}

SCRIPT_PATH=`realpath "$0"`
SCRIPT_DIR=`dirname "$SCRIPT_PATH"`
SUBMODULE_DIR="$SCRIPT_DIR/../note-editor"
DESTINATION_DIR="$SCRIPT_DIR/../app/src/main/assets/note-editor"
HASH_FILE="$DESTINATION_DIR/note_editor_hash.txt"

if ! git -C "$SCRIPT_DIR" submodule status "$SUBMODULE_DIR" | grep -qv '^-'; then
    echo "Error: The note-editor submodule is not initialized. Run:"
    echo "    git submodule update --init --recursive note-editor"
    exit 1
fi

CURRENT_HASH=`git -C "$SCRIPT_DIR" ls-tree --object-only HEAD "$SUBMODULE_DIR"`
if [ -z "$CURRENT_HASH" ]; then
    CURRENT_HASH=`git -C "$SCRIPT_DIR" ls-files -s "$SUBMODULE_DIR" | cut -d ' ' -f 2`
fi
if [ -z "$CURRENT_HASH" ]; then
    echo "Error: Could not determine the note-editor submodule commit."
    exit 1
fi

DOWNLOAD_URL="https://zotero-download.s3.amazonaws.com/ci/note-editor/${CURRENT_HASH}.zip"

BUILD_SOURCE_DIR="android"

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
curl -fL "$DOWNLOAD_URL" -o "$TMP_DIR/build.zip"

echo "Unzipping..."
unzip -q "$TMP_DIR/build.zip" -d "$TMP_DIR/build"

if [ ! -d "$TMP_DIR/build/$BUILD_SOURCE_DIR" ]; then
    echo "Error: $BUILD_SOURCE_DIR build not found in the archive."
    exit 1
fi

mkdir -p "$DESTINATION_DIR"

cd "$TMP_DIR/build/$BUILD_SOURCE_DIR"
zip -rq "$DESTINATION_DIR/note-editor.zip" "." -x "*.map"

echo "$CURRENT_HASH" > "$HASH_FILE"
echo "Build $BUILD_SOURCE_DIR installed at $DESTINATION_DIR from hash $CURRENT_HASH"

rm -rf "$TMP_DIR"
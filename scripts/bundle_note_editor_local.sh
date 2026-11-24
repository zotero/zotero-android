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

CURRENT_HASH="local-`date +%s`"

NOTE_EDITOR_BUILD_FOLDER="$SUBMODULE_DIR/build/android"

cd "$SUBMODULE_DIR"
if [ ! -d "node_modules" ]; then
    npm ci
fi
npm run build:android

rm -rf "$DESTINATION_DIR"
mkdir -p "$DESTINATION_DIR"

cd "$NOTE_EDITOR_BUILD_FOLDER"
zip -rq "$DESTINATION_DIR/note-editor.zip" "." -x "*.map"

echo "$CURRENT_HASH" > "$HASH_FILE"
echo "Local build installed at $DESTINATION_DIR with hash $CURRENT_HASH"
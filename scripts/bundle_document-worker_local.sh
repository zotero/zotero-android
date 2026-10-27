#!/bin/bash

set -eo pipefail

realpath() {
    [[ $1 = /* ]] && echo "$1" || echo "$PWD/${1#./}"
}

SCRIPT_PATH=`realpath "$0"`
SCRIPT_DIR=`dirname "$SCRIPT_PATH"`
SUBMODULE_DIR="$SCRIPT_DIR/../document-worker"
DESTINATION_DIR="$SCRIPT_DIR/../app/src/main/assets/document-worker"
HASH_FILE="$DESTINATION_DIR/document-worker_commit_hash.txt"
ANDROID_HOST_DIR="$SUBMODULE_DIR/test/runtime/android"

CURRENT_HASH=$RANDOM

DOCUMENT_WORKER_TO_ZIP_FOLDER="$SUBMODULE_DIR/build"

cd "$SUBMODULE_DIR"
npm ci
npm run build

cp "$ANDROID_HOST_DIR"/* "$DOCUMENT_WORKER_TO_ZIP_FOLDER"

mkdir -p "$DESTINATION_DIR"

cd "$DOCUMENT_WORKER_TO_ZIP_FOLDER"
rm -f "$DESTINATION_DIR/document-worker.zip"
zip -r "$DESTINATION_DIR/document-worker.zip" "."

echo "$CURRENT_HASH" > "$HASH_FILE"
echo "Local build installed at $DESTINATION_DIR from placeholder hash $CURRENT_HASH"

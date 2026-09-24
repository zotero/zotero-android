#!/bin/bash

set -eo pipefail

realpath() {
    [[ $1 = /* ]] && echo "$1" || echo "$PWD/${1#./}"
}

SCRIPT_PATH=`realpath "$0"`
SCRIPT_DIR=`dirname "$SCRIPT_PATH"`
SUBMODULE_DIR="$SCRIPT_DIR/../document-worker"
DESTINATION_DIR="$SCRIPT_DIR/../app/src/main/assets/document_worker"
HASH_FILE="$DESTINATION_DIR/document_worker_hash.txt"

if ! git -C "$SCRIPT_DIR" submodule status "$SUBMODULE_DIR" | grep -qv '^-'; then
    echo "Error: The document-worker submodule is not initialized. Run:"
    echo "    git submodule update --init --recursive document-worker"
    exit 1
fi

CURRENT_HASH=`git ls-tree --object-only HEAD "$SUBMODULE_DIR"`
DOWNLOAD_URL="https://zotero-download.s3.amazonaws.com/ci/document-worker/${CURRENT_HASH}.zip"

DOCUMENT_WORKER_TO_ZIP_FOLDER="$SCRIPT_DIR/../documentWorkerToZip"

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

mkdir -p "$DOCUMENT_WORKER_TO_ZIP_FOLDER"
mkdir -p "$DESTINATION_DIR"

shopt -s dotglob
cp -r "$TMP_DIR/build/"* "$DOCUMENT_WORKER_TO_ZIP_FOLDER"
shopt -u dotglob

cd "$DOCUMENT_WORKER_TO_ZIP_FOLDER"
zip -r "$DESTINATION_DIR/document_worker.zip" "."

echo "$CURRENT_HASH" > "$HASH_FILE"
echo "Build installed at $DESTINATION_DIR from hash $CURRENT_HASH"

rm -rf "$TMP_DIR"
rm -rf "$DOCUMENT_WORKER_TO_ZIP_FOLDER"
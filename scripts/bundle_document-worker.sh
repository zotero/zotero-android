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

mkdir -p "$DOCUMENT_WORKER_TO_ZIP_FOLDER"
mkdir -p "$DESTINATION_DIR"

TMP_DIR=$(mktemp -d)
echo "Created temp dir: $TMP_DIR"

DOWNLOADED=0
echo "Downloading build from: $DOWNLOAD_URL"
if curl -fL "$DOWNLOAD_URL" -o "$TMP_DIR/build.zip"; then
    echo "Unzipping..."
    unzip -q "$TMP_DIR/build.zip" -d "$DOCUMENT_WORKER_TO_ZIP_FOLDER"
    DOWNLOADED=1
else
    echo "Prebuilt document-worker artifact not found, building from source instead."
fi

if [ "$DOWNLOADED" -eq 0 ]; then
    (cd "$SUBMODULE_DIR" && npm ci && npm run build)
    shopt -s dotglob
    cp -r "$SUBMODULE_DIR/build/"* "$DOCUMENT_WORKER_TO_ZIP_FOLDER"
    shopt -u dotglob
fi

cp "$ANDROID_HOST_DIR"/* "$DOCUMENT_WORKER_TO_ZIP_FOLDER"

cd "$DOCUMENT_WORKER_TO_ZIP_FOLDER"
zip -r "$DESTINATION_DIR/document-worker.zip" "."

echo "$CURRENT_HASH" > "$HASH_FILE"
echo "Build installed at $DESTINATION_DIR from hash $CURRENT_HASH"

rm -rf "$TMP_DIR"
rm -rf "$DOCUMENT_WORKER_TO_ZIP_FOLDER"

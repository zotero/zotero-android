#!/bin/bash

set -eo pipefail

realpath() {
    [[ $1 = /* ]] && echo "$1" || echo "$PWD/${1#./}"
}

SCRIPT_PATH=`realpath "$0"`
SCRIPT_DIR=`dirname "$SCRIPT_PATH"`
UTILITIES_SUBMODULE_DIR="$SCRIPT_DIR/../utilities"
UTILITIES_DIR="$SCRIPT_DIR/../app/src/main/assets/utilities"
HASH_FILE="$UTILITIES_DIR/utilities_hash.txt"

UTILITIES_TO_ZIP_FOLDER="$SCRIPT_DIR/../utilitiesToZip"

# Check if the utilities submodule is initialized
if ! git -C "$SCRIPT_DIR" submodule status "$UTILITIES_SUBMODULE_DIR" | grep -qv '^-'; then
    echo "Error: The utilities submodule is not initialized. Run:"
    echo "    git submodule update --init --recursive utilities"
    exit 1
fi

CURRENT_HASH=`git ls-tree --object-only HEAD "$UTILITIES_SUBMODULE_DIR"`

if [ -d "$UTILITIES_DIR" ]; then
    if [ -f "$HASH_FILE" ]; then
        CACHED_HASH=`cat "$HASH_FILE"`
    else 
        CACHED_HASH=0
    fi

    if [ "$CACHED_HASH" == "$CURRENT_HASH" ]; then
        exit
    else
        rm -rf "$UTILITIES_DIR"
    fi
fi

mkdir -p "$UTILITIES_TO_ZIP_FOLDER"
mkdir -p "$UTILITIES_DIR"

cp -R "$UTILITIES_SUBMODULE_DIR"/[!.]* "$UTILITIES_TO_ZIP_FOLDER"
cd "$UTILITIES_TO_ZIP_FOLDER"
zip -r "$UTILITIES_DIR/utilities.zip" "."
rm -rf "$UTILITIES_TO_ZIP_FOLDER"
echo "$CURRENT_HASH" > "$HASH_FILE"

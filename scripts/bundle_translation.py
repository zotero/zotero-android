import json
import os
import re
import shutil
import subprocess
import time

def commit_hash_from_submodules(array):
    for line in array:
        if line.startswith("translation/translate"):
            return line.split()[1]

# Get bundle directory
bundle_dir = os.path.join(os.path.abspath("."), "app" + os.sep + "src" + os.sep + "main" + os.sep + "assets")

if not os.path.isdir(bundle_dir):
    os.mkdir(bundle_dir)

# Get translation directory
translators_dir = os.path.join(os.path.abspath("."), "translation")

if not os.path.isdir(translators_dir):
    raise Exception(translators_dir + " is not a directory. Call update_bundled_data.py first.")

# Store last commit hash from translation submodule
submodules = subprocess.check_output(["git", "submodule", "foreach", "--recursive", "echo $path `git rev-parse HEAD`"]).decode("utf-8").splitlines()
commit_hash = commit_hash_from_submodules(submodules)

with open(os.path.join(bundle_dir, "translation_commit_hash.txt"), "w") as f:
    f.write(str(commit_hash))

# Zip translator
os.chdir(translators_dir)
subprocess.check_call(['zip', '-r', os.path.join(bundle_dir, "translator.zip"), "."])

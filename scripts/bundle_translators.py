import json
import os
import re
import shutil
import subprocess
import time

def commit_hash_from_submodules(array):
    for line in array:
        if line.startswith("translators"):
            return line.split()[1]

def index_json(directory):
    index = []

    for fn in sorted((fn for fn in os.listdir(directory)), key=str.lower):
        if not fn.endswith(".js"):
            continue
        
        with open(os.path.join(directory, fn), 'r', encoding='utf-8') as f:
            contents = f.read()
            # Parse out the JSON metadata block
            m = re.match('^\s*{[\S\s]*?}\s*?[\r\n]', contents)
            
            if not m:
                raise Exception("Metadata block not found in " + f.name)
            
            metadata = json.loads(m.group(0))
            
            index.append({"id": metadata["translatorID"],
                          "fileName": fn,
                          "lastUpdated": metadata["lastUpdated"]})

    return index

# Get bundle directory
bundle_dir = os.path.join(os.path.abspath("."), "app" + os.sep + "src" + os.sep + "main" + os.sep + "assets" + os.sep + "translators")

if not os.path.isdir(bundle_dir):
    os.mkdir(bundle_dir)

# Get translators directory
translators_dir = os.path.join(os.path.abspath("."), "translators")

if not os.path.isdir(translators_dir):
    raise Exception(translators_dir + " is not a directory. Call update_bundled_data.py first.")

# Store last commit hash from translators submodule
submodules = subprocess.check_output(["git", "submodule", "foreach", "--recursive", "echo $path `git rev-parse HEAD`"]).decode("utf-8").splitlines()
commit_hash = commit_hash_from_submodules(submodules)

with open(os.path.join(bundle_dir, "commit_hash.txt"), "w") as f:
    f.write(str(commit_hash))

# Copy deleted.txt to bundle
shutil.copyfile(os.path.join(translators_dir, "deleted.txt"), os.path.join(bundle_dir, "deleted.txt"))

# Create index file
index = index_json(translators_dir)
with open(os.path.join(bundle_dir, "index.json"), "w") as f:
    json.dump(index, f, indent=True, ensure_ascii=False)

# Zip translators
os.chdir(translators_dir)
subprocess.check_call(['zip', '-r', os.path.join(bundle_dir, "translators.zip"), "."])

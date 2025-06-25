import os
import shutil
import subprocess
import sys

submodule_path_parts = ["bundled-styles"]
stylesToZipSubFolderName = "stylesToZip"
targetFolderToSaveTo = os.path.join(os.path.abspath("."), "app" + os.sep + "src" + os.sep + "main" + os.sep + "assets" + os.sep + "styles")

submodule_path = os.path.join(*submodule_path_parts)

def get_canonical_submodule_hash(submodule_path: str) -> str:
    output = subprocess.check_output([
        "git", "ls-tree", "--object-only", "HEAD", submodule_path
    ])
    return output.decode("utf-8").strip()

def get_actual_submodule_hash(submodule_path: str) -> str:
    output = subprocess.check_output([
        "git", "-C", submodule_path, "rev-parse", "HEAD"
    ])
    return output.decode("utf-8").strip()

def read_existing_commit_hash(path: str) -> str:
    if os.path.isfile(path):
        with open(path, "r") as f:
            return f.read().strip()
    return ""

def write_commit_hash(path: str, commit_hash: str):
    with open(path, "w") as f:
        f.write(commit_hash)

# Get bundle directory
stylesToZipSubFolder = os.path.join(os.path.abspath("."), stylesToZipSubFolderName)
hash_path = os.path.join(targetFolderToSaveTo, "commit_hash.txt")

if not os.path.isdir(targetFolderToSaveTo):
    os.mkdir(targetFolderToSaveTo)

if not os.path.isdir(stylesToZipSubFolder):
    os.mkdir(stylesToZipSubFolder)

# Get submodule directory
submodule_dir = os.path.join(os.path.abspath("."), submodule_path)

if not os.path.isdir(submodule_dir):
    raise Exception(submodule_dir + " is not a directory. Init submodules first.")

existing_hash = read_existing_commit_hash(hash_path)
current_hash = get_actual_submodule_hash(submodule_path)

if existing_hash == current_hash:
    print("Bundle already up to date")
    sys.exit(0)

# Copy files to bundle
for filename in os.listdir(submodule_dir):
    if filename.endswith(".csl"):
        shutil.copyfile(os.path.join(submodule_dir, filename), os.path.join(stylesToZipSubFolder, filename))
        continue
    else:
        continue

write_commit_hash(hash_path, current_hash)

os.chdir(stylesToZipSubFolder)
subprocess.check_call(['zip', '-r', os.path.join(targetFolderToSaveTo, "styles.zip"), "."])
shutil.rmtree(stylesToZipSubFolder)
print("Bundle " + stylesToZipSubFolderName + " copied from hash " + current_hash)
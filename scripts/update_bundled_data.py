import os
import shutil
import subprocess
import time

# Get bundle directory
bundle_dir = os.path.join(os.path.abspath("."), "app" + os.sep + "src" + os.sep + "main" + os.sep + "assets")

if not os.path.isdir(bundle_dir):
    raise Exception(bundle_dir + " is not a directory")

# Update translators submodule
subprocess.check_call(["git", "pull", "--recurse-submodules"])
subprocess.check_call(["git", "submodule", "update", "--recursive", "--remote"])

# Store timestamp
timestamp = int(time.time())
with open(os.path.join(bundle_dir, "timestamp.txt"), "w") as f:
    f.write(str(timestamp))

import json
import os
import re
import shutil
import subprocess
import time
import urllib.request

citation_proc_version="2"

bundle_dir = os.path.join(os.path.abspath("."), "app" + os.sep + "src" + os.sep + "main" + os.sep + "assets" + os.sep + "citation")

if not os.path.isdir(bundle_dir):
    os.mkdir(bundle_dir)

citation_proc_dir = os.path.join(os.path.abspath("."), "citation_proc")

with open(os.path.join(bundle_dir, "citation_proc_commit_hash.txt"), "w") as f:
    f.write(str(citation_proc_version))

os.chdir(citation_proc_dir)
subprocess.check_call(['zip', '-r', os.path.join(bundle_dir, "citation_proc.zip"), "."])

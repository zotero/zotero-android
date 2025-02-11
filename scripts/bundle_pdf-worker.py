import json
import os
import re
import shutil
import subprocess
import time
import urllib.request

commit_hash = "897513ca6427e148d36c9105f1f54f5afc5e28f6"

pdf_worker_download_url = "https://zotero-download.s3.amazonaws.com/ci/client-pdf-worker/" + commit_hash + ".zip"

# Get bundle directory
bundle_dir = os.path.join(os.path.abspath("."), "app" + os.sep + "src" + os.sep + "main" + os.sep + "assets")

if not os.path.isdir(bundle_dir):
    os.mkdir(bundle_dir)

# Get pdf-worker directory
pdf_worker_dir = os.path.join(os.path.abspath("."), "pdf-worker")

if not os.path.isdir(pdf_worker_dir):
    raise Exception(pdf_worker_dir + " is not a directory. Call update_bundled_data.py first.")

with open(os.path.join(bundle_dir, "pdf-worker_commit_hash.txt"), "w") as f:
    f.write(str(commit_hash))

# Download cmaps and standard_fonts
cmaps_and_fonts_zip_file = os.path.join(pdf_worker_dir, "pdf_worker_cmaps_and_fonts.zip")
urllib.request.urlretrieve(pdf_worker_download_url, cmaps_and_fonts_zip_file)

# Extract cmaps and standard_fonts
subprocess.check_call(['unzip', cmaps_and_fonts_zip_file, "-d", pdf_worker_dir])
os.remove(cmaps_and_fonts_zip_file)

# Zip custom pdf-worker wrapper together with cmaps and standard_fonts
os.chdir(pdf_worker_dir)
subprocess.check_call(['zip', '-r', os.path.join(bundle_dir, "pdf-worker.zip"), "."])

package org.zotero.android.androidx.file

import java.io.File

fun File.copyWithExt(ext: String): File {
    return File(this.parent, this.nameWithoutExtension + "." + ext)
}
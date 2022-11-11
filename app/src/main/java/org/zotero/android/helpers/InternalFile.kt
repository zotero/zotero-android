package org.zotero.android.helpers

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.File

@Parcelize
data class InternalFile(
    val mimeType: MimeType,
    val file: File,
    val canonicalPath: String
) : Parcelable {
    val asUriString: String
        get() = Uri.fromFile(file).toString()
}
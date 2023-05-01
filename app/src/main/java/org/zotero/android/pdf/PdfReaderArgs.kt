package org.zotero.android.pdf

import android.net.Uri
import org.zotero.android.sync.Library

data class PdfReaderArgs(
    val key: String,
    val library: Library,
    val uri: Uri,
    val page: Int?,
    val preselectedAnnotationKey: String?,
)

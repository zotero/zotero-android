package org.zotero.android.screens.htmlepub.reader.data

import android.net.Uri
import org.zotero.android.sync.Library

data class HtmlEpubReaderArgs(
    val key: String,
    val parentKey: String?,
    val library: Library,
    val uri: Uri,
)

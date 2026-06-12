package org.zotero.android.screens.reader.data

import android.net.Uri
import org.zotero.android.sync.Library

data class ReaderArgs(
    val key: String,
    val parentKey: String?,
    val library: Library,
    val uri: Uri,
)

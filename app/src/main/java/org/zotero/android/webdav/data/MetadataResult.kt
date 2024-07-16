package org.zotero.android.webdav.data

sealed class MetadataResult {
    object unchanged : MetadataResult()
    data class mtimeChanged(val mtime: Long) : MetadataResult()
    data class changed(val url: String) : MetadataResult()
    data class new(val url: String) : MetadataResult()
}
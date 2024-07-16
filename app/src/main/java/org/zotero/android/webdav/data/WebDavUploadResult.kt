package org.zotero.android.webdav.data

import java.io.File

sealed class WebDavUploadResult {
    object exists : WebDavUploadResult()
    data class new(val url: String, val file: File) : WebDavUploadResult()
}
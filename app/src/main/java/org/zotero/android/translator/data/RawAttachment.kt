package org.zotero.android.translator.data

import android.net.Uri

sealed interface RawAttachment {
    data class web(
        val title: String,
        val url: String,
        val html: String,
        val cookies: String,
        val frames: List<String>,
        val userAgent: String,
        val referrer: String
    ) : RawAttachment

    data class remoteUrl(val url: String) : RawAttachment
    data class fileUrl(val uri: Uri) : RawAttachment
    data class remoteFileUrl(
        val url: String,
        val contentType: String?,
        val cookies: String?,
        val userAgent: String?,
        val referrer: String?
    ) : RawAttachment
}
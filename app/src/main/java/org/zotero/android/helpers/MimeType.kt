package org.zotero.android.helpers

typealias MimeType = String

fun MimeType.isVideo() = split("/")[0] == "video"

fun MimeType.isImage() = split("/")[0] == "image"

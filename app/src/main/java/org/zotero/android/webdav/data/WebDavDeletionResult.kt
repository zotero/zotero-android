package org.zotero.android.webdav.data

data class WebDavDeletionResult(
    val succeeded: Set<String>,
    val missing: Set<String>,
    val failed: Set<String>,
)
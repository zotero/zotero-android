package org.zotero.android.screens.htmlepub.reader.data

data class DocumentUpdate(
    val deletions: List<String>,
    val insertions: List<Map<String, Any>>,
    val modifications: List<Map<String, Any>>,
)
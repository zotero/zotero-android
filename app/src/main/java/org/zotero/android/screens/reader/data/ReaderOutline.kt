package org.zotero.android.screens.reader.data

data class ReaderOutline(
    val title: String,
    val location: Map<String, Any>,
    val children: List<ReaderOutline>,
    val isActive: Boolean
) {
}
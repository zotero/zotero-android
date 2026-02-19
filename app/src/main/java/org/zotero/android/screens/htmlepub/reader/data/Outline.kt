package org.zotero.android.screens.htmlepub.reader.data

data class Outline(
    val title: String,
    val location: Map<String, Any>,
    val children: List<Outline>,
    val isActive: Boolean
) {
}
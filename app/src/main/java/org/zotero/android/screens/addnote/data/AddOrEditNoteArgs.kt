package org.zotero.android.screens.addnote.data

import org.zotero.android.sync.Library

data class AddOrEditNoteArgs(
    val title: TitleData?,
    val key: String,
    val library: Library,
    val readOnly: Boolean,
    val parentKey: String? = null,
    val collectionKey: String? = null,
) {
    data class TitleData(
        val type: String,
        val title: String,
    )
}
package org.zotero.android.screens.addnote.data

import org.zotero.android.sync.Tag

data class SaveNoteAction(
    val text: String,
    val tags: List<Tag>,
    val key: String,
    val isFromDashboard: Boolean,
)

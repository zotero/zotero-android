package org.zotero.android.dashboard.data

import org.zotero.android.sync.Tag

data class SaveNoteAction(
    val text: String,
    val tags: List<Tag>,
    val key: String,
    val isFromDashboard: Boolean,
)

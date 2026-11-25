package org.zotero.android.screens.addnote.data

import org.zotero.android.sync.Tag

data class NoteSavedAction(
    val key: String,
    val text: String,
    val tags: List<Tag>,
    val parentKey: String?,
)
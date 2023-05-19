package org.zotero.android.screens.allitems.data

import org.zotero.android.sync.Tag

sealed class ItemsFilter {
    object downloadedFiles : ItemsFilter()
    data class tags(val tags: List<Tag>) : ItemsFilter()
}
package org.zotero.android.screens.allitems.data

sealed class ItemsFilter {
    object downloadedFiles : ItemsFilter()
    data class tags(val tags: Set<String>) : ItemsFilter()
}
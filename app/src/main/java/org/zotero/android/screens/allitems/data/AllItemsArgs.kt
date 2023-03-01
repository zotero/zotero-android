package org.zotero.android.screens.allitems.data
import org.zotero.android.sync.Collection
import org.zotero.android.sync.Library

data class AllItemsArgs(
    val collection: Collection,
    val library: Library,
    val sortType: ItemsSortType,
    val searchTerm: String?,
    val error: ItemsError?
)
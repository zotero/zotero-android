package org.zotero.android.screens.collections.data
import  org.zotero.android.sync.Collection

data class SearchableCollection(
    val isActive: Boolean,
    val collection: Collection
) {
    fun isActive(isActive: Boolean): SearchableCollection{
        return SearchableCollection(isActive = isActive, collection = this.collection)
    }
}

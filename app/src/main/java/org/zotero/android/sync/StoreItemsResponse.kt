package org.zotero.android.sync

import org.zotero.android.api.pojo.sync.ItemResponse

class StoreItemsResponse(
    val changedFilenames: List<FilenameChange>,
    val conflicts: List<Error>
) {
    data class FilenameChange(
        val key: String,
        val oldName: String,
        val newName: String,
        val contentType: String
    )

    sealed class Error : Throwable() {
        data class itemDeleted(val itemResponse: ItemResponse) : Error()
        data class itemChanged(val itemResponse: ItemResponse) : Error()
        data class noValidCreators(val key: String, val itemType: String): Error()
        data class invalidCreator(val key: String, val creatorType: String): Error()
    }
}
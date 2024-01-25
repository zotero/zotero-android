package org.zotero.android.screens.allitems.processor

import androidx.compose.runtime.snapshots.SnapshotStateList
import org.zotero.android.architecture.LCE2
import org.zotero.android.database.objects.Attachment
import org.zotero.android.screens.allitems.data.ItemCellModel
import org.zotero.android.screens.allitems.data.ItemsError
import org.zotero.android.screens.allitems.data.ItemsFilter
import org.zotero.android.screens.itemdetails.data.DetailType
import org.zotero.android.sync.Collection
import org.zotero.android.sync.Library

interface AllItemsProcessorInterface {

    fun currentLibrary(): Library
    fun currentCollection(): Collection
    fun currentSearchTerm(): String?
    fun currentFilters(): List<ItemsFilter>

    fun show(attachment: Attachment, library: Library)
    fun updateTagFilter()
    fun isEditing(): Boolean
    fun showItemDetailWithDelay(creation: DetailType.creation)
    fun updateItemCellModels(itemCellModels: SnapshotStateList<ItemCellModel>)

    fun updateLCE(lce: LCE2)
    fun showError(error: ItemsError)
}
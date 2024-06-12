package org.zotero.android.screens.allitems.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.Color
import org.zotero.android.database.objects.RItem
import org.zotero.android.uicomponents.attachmentprogress.State

data class ItemCellModel(
    val key: String,
    val typeIconName: String,
    val typeName: String,
    val title: String,
    val subtitle: String,
    val hasNote: Boolean,
    val accessory: Accessory?,
    val tagColors: SnapshotStateList<Color>,
) {

    sealed class Accessory {
        data class attachment(val state: State) : Accessory()
        object doi : Accessory()
        object url : Accessory()
    }

    companion object {
        fun init(item: RItem, typeName: String, accessory: Accessory?): ItemCellModel {
            val dbRow = item.allItemsDbRow!!
            val hasNote = dbRow.hasNote
            return ItemCellModel(
                key = item.key,
                typeIconName = dbRow.typeIconName,
                typeName = typeName,
                title = dbRow.title,
                subtitle = dbRow.subtitle,
                hasNote = hasNote,
                accessory = accessory,
                tagColors = mutableStateListOf()
            )
        }

    }
    fun updateAccessory(cellAccessory: Accessory?): ItemCellModel {
        return copy(accessory = cellAccessory)
    }
}
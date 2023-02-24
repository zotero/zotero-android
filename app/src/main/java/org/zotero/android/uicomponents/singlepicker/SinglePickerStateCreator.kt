package org.zotero.android.uicomponents.singlepicker

import org.zotero.android.database.objects.ItemTypes
import org.zotero.android.sync.SchemaController

object SinglePickerStateCreator {
    fun create(selected: String, schemaController: SchemaController): SinglePickerState {
        val types = schemaController.itemTypes.mapNotNull { type ->
            if (ItemTypes.excludedFromTypePicker.contains(type)) {
                return@mapNotNull null
            }
            val name = schemaController.localizedItemType(type)
            if (name == null) {
                return@mapNotNull null
            }

            SinglePickerItem(id = type, name = name)
        }.sortedBy { it.name }
        return SinglePickerState(objects = types, selectedRow = selected)
    }
}
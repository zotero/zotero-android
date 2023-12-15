package org.zotero.android.screens.creatoredit.data

import org.zotero.android.screens.itemdetails.data.ItemDetailCreator

data class CreatorEditArgs(
    val creator: ItemDetailCreator,
    val itemType: String,
    val isEditing: Boolean,
)
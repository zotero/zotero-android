package org.zotero.android.screens.collectionpicker.data

import org.zotero.android.sync.Collection

data class CollectionPickerSingleResult(val collection: Collection)
data class CollectionPickerMultiResult(val keys: Set<String>)

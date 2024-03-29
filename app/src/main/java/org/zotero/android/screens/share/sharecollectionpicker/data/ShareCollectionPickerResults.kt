package org.zotero.android.screens.share.sharecollectionpicker.data

import org.zotero.android.sync.Collection
import org.zotero.android.sync.Library

data class ShareCollectionPickerResults(
    val collection: Collection?,
    val library: Library,
    )


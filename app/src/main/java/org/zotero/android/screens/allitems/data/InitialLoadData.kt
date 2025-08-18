package org.zotero.android.screens.allitems.data

import org.zotero.android.sync.Collection
import org.zotero.android.sync.Library

data class InitialLoadData(
    val collection: Collection,
    val library: Library
)
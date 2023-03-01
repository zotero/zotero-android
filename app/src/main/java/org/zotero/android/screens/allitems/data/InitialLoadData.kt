package org.zotero.android.screens.allitems.data

import org.zotero.android.sync.Library
import org.zotero.android.sync.Collection

data class InitialLoadData(
    val collection: Collection,
    val library: Library
) {
}
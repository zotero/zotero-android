package org.zotero.android.screens.collectionedit.data

import org.zotero.android.sync.Collection
import org.zotero.android.sync.Library

data class CollectionEditArgs(
    val library: Library,
    val key: String?,
    val name: String,
    val parent: Collection?
)
package org.zotero.android.screens.collections.data

import org.zotero.android.sync.Collection

data class CollectionItemWithChildren(
    val collection: Collection,
    val children: List<CollectionItemWithChildren>
)

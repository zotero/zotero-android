package org.zotero.android.screens.collections.data

import kotlinx.collections.immutable.ImmutableList
import org.zotero.android.sync.Collection

data class CollectionItemWithChildren(
    val collection: Collection,
    val children: ImmutableList<CollectionItemWithChildren>
)

package org.zotero.android.screens.collections.data

import org.zotero.android.sync.Collection
import java.util.TreeSet

data class CollectionTreeNode(
    var parentKey: String?,
    var collection: Collection,
    val children: TreeSet<CollectionTreeNode> = TreeSet<CollectionTreeNode>(CollectionTreeComparator())
)

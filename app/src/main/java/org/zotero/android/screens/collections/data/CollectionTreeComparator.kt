package org.zotero.android.screens.collections.data

class CollectionTreeComparator : Comparator<CollectionTreeNode> {
    override fun compare(n1: CollectionTreeNode, n2: CollectionTreeNode): Int {
        return n1.collection.name.lowercase().compareTo(n2.collection.name.lowercase())
    }
}
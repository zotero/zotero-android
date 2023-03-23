package org.zotero.android.screens.collections.data

import org.zotero.android.sync.Collection
import org.zotero.android.sync.CollectionIdentifier

data class CollectionTree(
    var nodes: MutableList<Node>,
    var collections: MutableMap<CollectionIdentifier, Collection>,
    var collapsed: MutableMap<CollectionIdentifier, Boolean>,
    var filtered: MutableMap<CollectionIdentifier, SearchableCollection> = mutableMapOf(),
) {
    data class Node(
        val identifier: CollectionIdentifier,
        val parent: CollectionIdentifier?,
        val children: List<Node>,
    )

    enum class CollapseState {
        expandedAll,
        collapsedAll,
        basedOnDb,
    }

    fun append(collection: Collection, collapsed: Boolean = true) {
        this.collections[collection.identifier] = collection
        this.collapsed[collection.identifier] = collapsed
        this.nodes.add(
            Node(
                identifier = collection.identifier,
                parent = null,
                children = emptyList()
            )
        )
    }

    fun insert(collection: Collection, collapsed: Boolean = true, index: Int) {
        this.collections[collection.identifier] = collection
        this.collapsed[collection.identifier] = collapsed
        this.nodes.add(
            index = index,
            element = Node(
                identifier = collection.identifier,
                parent = null,
                children = emptyList()
            )
        )
    }
    fun update(collection: Collection) {
        this.collections[collection.identifier] = collection
    }

    fun replace(matching: (CollectionIdentifier) -> Boolean, tree: CollectionTree) {
        replaceValues(this.collections, tree.collections, matching)
        replaceValues(this.collapsed,  tree.collapsed,  matching)
        replaceNodes(this.nodes,  tree.nodes,  matching)
    }

    private fun  <T> replaceValues(dictionary : MutableMap<CollectionIdentifier, T>, newDictionary: MutableMap<CollectionIdentifier, T>, matchingId: (CollectionIdentifier) -> Boolean) {
        for (key in dictionary.keys) {
            if (!matchingId(key)) { continue }
            dictionary.remove(key)
        }

        for ((key, value) in newDictionary) {
            dictionary[key] = value
        }
    }

    private fun replaceNodes(array: MutableList<Node>, newArray: List<Node>, matchingId: (CollectionIdentifier) -> Boolean) {
        var startIndex = -1
        var endIndex = -1

        for ((idx, node) in array.withIndex()) {
            if (startIndex == -1) {
                if (matchingId(node.identifier)) {
                    startIndex = idx
                }
            } else if (endIndex == -1) {
                if (!matchingId(node.identifier)) {
                    endIndex = idx
                    break
                }
            }
        }

        if (startIndex == -1) {
            // No object of given type found, insert after .all
            array.addAll(index = 1, newArray)
            return
        }

        if (endIndex == -1) {
            endIndex = array.size
        }
        for (i in endIndex - 1 downTo startIndex) {
            array.removeAt(i)
        }

        array.addAll(index = startIndex, newArray)
    }
    fun collection(identifier: CollectionIdentifier): Collection? {
        return this.collections[identifier]
    }
}

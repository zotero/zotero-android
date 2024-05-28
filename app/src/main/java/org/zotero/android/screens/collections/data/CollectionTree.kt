package org.zotero.android.screens.collections.data

import kotlinx.collections.immutable.toImmutableList
import org.zotero.android.sync.Collection
import org.zotero.android.sync.CollectionIdentifier
import java.util.concurrent.ConcurrentHashMap

data class CollectionTree(
    var nodes: MutableList<Node>,
    var collections: ConcurrentHashMap<CollectionIdentifier, Collection>,
    var collapsed: ConcurrentHashMap<CollectionIdentifier, Boolean>,
    var filtered: ConcurrentHashMap<CollectionIdentifier, SearchableCollection> = ConcurrentHashMap(),
) {
    data class Node(
        val identifier: CollectionIdentifier,
        val parent: CollectionIdentifier?,
        var children: List<Node>,
    )

    enum class CollapseState {
        expandedAll,
        collapsedAll,
        basedOnDb,
    }

    fun set(collapsed: Boolean, identifier: CollectionIdentifier) {
        this.collapsed[identifier] = collapsed
    }
    fun expandAllCollections() {
        for (identifier in this.collections.keys()) {
            set(false, identifier)
        }
    }
    fun collapseAllCollections() {
        for (identifier in this.collections.keys()) {
            set(true, identifier)
        }
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

    fun createSnapshot(): List<CollectionItemWithChildren> {
        return this.nodes.map { node ->
            add(node, this.collections)
        }
    }

    private fun add(
        currentNode: Node,
        allCollections: Map<CollectionIdentifier, Collection>
    ): CollectionItemWithChildren {
        val currentCollection = allCollections[currentNode.identifier]!!
        val children: MutableList<CollectionItemWithChildren> = mutableListOf()
        for (n in currentNode.children) {
            children.add(add(currentNode = n, allCollections = allCollections))
        }
        return CollectionItemWithChildren(collection = currentCollection, children = children.toImmutableList())
    }
    fun parent(identifier: CollectionIdentifier): CollectionIdentifier? {
        return firstNode(matching ={ node -> node.children.any{it.identifier == identifier } }, array = this.nodes)?.identifier
    }

    private fun firstNode(matching: (Node) -> Boolean, array: List<Node>): Node? {
        val queue = array.toMutableList()
        while (!queue.isEmpty()) {
            val node = queue.removeFirst()

            if (matching(node)) {
                return node
            }

            if (!node.children.isEmpty()) {
                queue.addAll(node.children)
            }
        }
        return null
    }

    fun sortNodes() {
        nodes.sortBy { this.collections[it.identifier]!!.name.lowercase() }
        nodes.forEach { node ->
            recursivelySortChildren(node)
        }
    }

    private fun recursivelySortChildren(node: Node) {
        val childsSorted =
            node.children
                .sortedBy { this.collections[it.identifier]!!.name.lowercase() }
        node.children = childsSorted
        node.children.forEach {
            recursivelySortChildren(it)
        }
    }

}

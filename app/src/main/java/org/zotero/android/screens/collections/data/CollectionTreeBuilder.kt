package org.zotero.android.screens.collections.data

import io.realm.RealmResults
import org.zotero.android.database.objects.RCollection
import org.zotero.android.database.objects.RSearch
import org.zotero.android.database.requests.items
import org.zotero.android.database.requests.parentKey
import org.zotero.android.database.requests.parentKeyNil
import org.zotero.android.sync.Collection
import org.zotero.android.sync.CollectionIdentifier
import org.zotero.android.sync.LibraryIdentifier

class CollectionTreeBuilder {
    data class Result(
        val collections: Map<CollectionIdentifier, Collection>,
        val root: List<CollectionIdentifier>,
        val children: Map<CollectionIdentifier, List<CollectionIdentifier>>,
        val collapsed: Map<CollectionIdentifier, Boolean>,
    )

    companion object {
        fun collections(searches: RealmResults<RSearch>): List<Collection> {
            return searches.map { Collection.initWithSearch(it) }
        }

        fun collections(
            rCollections: RealmResults<RCollection>,
            libraryId: LibraryIdentifier,
            includeItemCounts: Boolean
        ): CollectionTree {
            val collections = mutableMapOf<CollectionIdentifier, Collection>()
            val collapsed = mutableMapOf<CollectionIdentifier, Boolean>()
            val nodes: List<CollectionTree.Node> = collections(
                null,
                rCollections,
                libraryId = libraryId,
                includeItemCounts = includeItemCounts,
                allCollections = collections,
                collapsedState = collapsed
            )
            return CollectionTree(nodes = nodes.toMutableList(), collections = collections, collapsed = collapsed)
        }

        private fun collections(
            parent: CollectionIdentifier?,
            rCollections: RealmResults<RCollection>,
            libraryId: LibraryIdentifier,
            includeItemCounts: Boolean,
            allCollections: MutableMap<CollectionIdentifier, Collection>,
            collapsedState: MutableMap<CollectionIdentifier, Boolean>
        ): List<CollectionTree.Node> {
            val nodes = mutableListOf<CollectionTree.Node>()
            val parentKey = parent?.keyGet
            val filteredCollections = (if (parentKey != null) {
                rCollections.where().parentKey(parentKey)
            } else rCollections.where().parentKeyNil()).findAll()
            for (rCollection in filteredCollections) {
                val collection = collection(
                    rCollection = rCollection,
                    libraryId = libraryId,
                    includeItemCounts = includeItemCounts
                )
                allCollections[collection.identifier] = collection
                collapsedState[collection.identifier] = rCollection.collapsed

                val children = collections(
                    parent = collection.identifier,
                    rCollections = rCollections,
                    libraryId = libraryId,
                    includeItemCounts = includeItemCounts,
                    allCollections = allCollections,
                    collapsedState = collapsedState
                )
                val node = CollectionTree.Node(
                    identifier = collection.identifier,
                    parent = parent,
                    children = children
                )
                val insertionIndex =
                    insertionIndex(node = node, nodes, collections = allCollections)
                nodes.add(index = insertionIndex, node)
            }
            return nodes
        }

        private fun insertionIndex(node: CollectionTree.Node, array: MutableList<CollectionTree.Node>, collections: Map<CollectionIdentifier, Collection>): Int {
            return array.sortedWith {lhs, rhs ->
                val lCollection = collections[lhs.identifier] ?: return@sortedWith 1
                val rCollection = collections[rhs.identifier] ?: return@sortedWith 1
                lCollection.name.compareTo(rCollection.name)
            }.indexOf(node)
        }

        private fun collection(
            rCollection: RCollection,
            libraryId: LibraryIdentifier,
            includeItemCounts: Boolean
        ): Collection {
            var itemCount: Int = 0
            if (includeItemCounts) {
                itemCount = if (rCollection.items.size == 0) 0 else rCollection.items.where()
                    .items(CollectionIdentifier.collection(rCollection.key), libraryId = libraryId)
                    .count().toInt()
            }
            return Collection.initWithCollection(objectS = rCollection, itemCount = itemCount)
        }
    }


}
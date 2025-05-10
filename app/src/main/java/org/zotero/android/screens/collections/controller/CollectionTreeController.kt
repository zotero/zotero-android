package org.zotero.android.screens.collections.controller

import io.realm.OrderedCollectionChangeSet
import io.realm.RealmResults
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.database.objects.RCollection
import org.zotero.android.database.requests.items
import org.zotero.android.screens.collections.data.CollectionItemWithChildren
import org.zotero.android.screens.collections.data.CollectionTreeComparator
import org.zotero.android.screens.collections.data.CollectionTreeNode
import org.zotero.android.sync.Collection
import org.zotero.android.sync.CollectionIdentifier
import org.zotero.android.sync.LibraryIdentifier
import java.util.LinkedList
import java.util.TreeSet
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

class CollectionTreeController @Inject constructor(dispatchers: Dispatchers){

    private val collectionsFromDb: MutableList<Collection> = mutableListOf()

    private val nodes: TreeSet<CollectionTreeNode> = TreeSet<CollectionTreeNode>(CollectionTreeComparator())
    private val nodesWithNoPathToParent = mutableSetOf<CollectionTreeNode>()

    private val collections: ConcurrentHashMap<CollectionIdentifier, Collection> = ConcurrentHashMap()
    private val collapsed: ConcurrentHashMap<CollectionIdentifier, Boolean> = ConcurrentHashMap()

    private val coroutineScope = CoroutineScope(dispatchers.io)
    private val syncSchedulerSemaphore = Semaphore(1)

    private lateinit var libraryId: LibraryIdentifier
    private var includeItemCounts: Boolean = false
    private lateinit var collectionTreeControllerInterface: CollectionTreeControllerInterface

    private var ignoreNextDbModification: Boolean = false

    fun init(
        libraryId: LibraryIdentifier,
        includeItemCounts: Boolean,
        collectionTreeControllerInterface: CollectionTreeControllerInterface,
    ) {
        this.libraryId = libraryId
        this.includeItemCounts = includeItemCounts
        this.collectionTreeControllerInterface = collectionTreeControllerInterface
    }

    fun reactToCollectionsDbUpdate(
        collections: RealmResults<RCollection>,
        changeSet: OrderedCollectionChangeSet,
    ) {
        val frozenCollections = collections.freeze()

        val deletions = changeSet.deletions
        var insertions = changeSet.insertions
        val modifications = changeSet.changes

        if (deletions.isEmpty() && insertions.isEmpty() && modifications.isEmpty()) {
            insertions = IntArray(frozenCollections.size) { it }
        }
        processCollectionUpdate(
            frozenCollections = frozenCollections,
            deletions = deletions,
            insertions = insertions,
            modifications = modifications
        )
    }

    private fun processCollectionUpdate(
        frozenCollections: RealmResults<RCollection>,
        deletions: IntArray,
        insertions: IntArray,
        modifications: IntArray
    ) {
        val updateThreshold = 200
        var currentProcessingCount = 0

        coroutineScope.launch {
            syncSchedulerSemaphore.withPermit {
                val collectionsToUpdate = collectionsFromDb
                deletions.sorted().reversed().forEach { idx ->
                    if (!isActive) {
                        return@launch
                    }
                    val collectionToRemove = collectionsToUpdate.removeAt(idx)
                    collections.remove(collectionToRemove.identifier)
                    collapsed.remove(collectionToRemove.identifier)

                    removeNode(collectionToRemove.identifier)

                    //We don't count deletions towards updateThreshold as they are very fast.
                }

                insertions.forEach { idx ->
                    if (!isActive) {
                        return@launch
                    }
                    val item = frozenCollections[idx]!!

                    val generatedCollection = generateCollectionAndUpdateCollectionsFromDbList(
                        rCollection = item,
                        insertionIndex = idx
                    )
                    collections[generatedCollection.identifier] = generatedCollection
                    collapsed[generatedCollection.identifier] = item.collapsed

                    insertNode(dbCollection = item, generatedCollection = generatedCollection)

                    if (!isActive) {
                        return@launch
                    }
                    currentProcessingCount++
                    if (currentProcessingCount % updateThreshold == 0) {
                        sendChangedToUi(includeTreeChanges = true, includeCollapsedChanges = true)
                    }
                }
                modifications.forEach { idx ->
                    if (!isActive) {
                        return@launch
                    }
                    if (ignoreNextDbModification) {
                        ignoreNextDbModification = false
                        return@withPermit
                    }

                    val item = frozenCollections[idx]!!

                    val generatedCollection = generateCollectionAndUpdateCollectionsFromDbList(
                        rCollection = item,
                        modificationIndex = idx
                    )

                    collections[generatedCollection.identifier] = generatedCollection
                    collapsed[generatedCollection.identifier] = item.collapsed

                    val removedNode = removeNode(generatedCollection.identifier)
                    insertNode(
                        dbCollection = item,
                        generatedCollection = generatedCollection,
                        childrenBeforeDeletion = removedNode?.children
                    )

                    if (!isActive) {
                        return@launch
                    }
                    currentProcessingCount++
                    if (currentProcessingCount % updateThreshold == 0) {
                        sendChangedToUi(includeTreeChanges = true, includeCollapsedChanges = true)
                    }
                }
                if (!isActive) {
                    return@launch
                }
                sendChangedToUi(includeTreeChanges = true, includeCollapsedChanges = true)
            }
        }
    }

    private fun CoroutineScope.generateCollectionAndUpdateCollectionsFromDbList(
        rCollection: RCollection,
        insertionIndex: Int? = null,
        modificationIndex: Int? = null,
    ): Collection {
        val generatedCollection = collection(rCollection = rCollection)
        if (modificationIndex != null) {
            collectionsFromDb[modificationIndex] = generatedCollection
        } else if (insertionIndex != null) {
            collectionsFromDb.add(insertionIndex, generatedCollection)
        } else {
            collectionsFromDb.add(generatedCollection)
        }
        return generatedCollection
    }

    private fun collection(
        rCollection: RCollection,
    ): Collection {
        var itemCount = 0
        if (includeItemCounts) {
            itemCount = if (rCollection.items.isEmpty()) 0 else rCollection.items.where()
                .items(CollectionIdentifier.collection(rCollection.key), libraryId = libraryId)
                .count().toInt()
        }
        return Collection.initWithCollection(objectS = rCollection, itemCount = itemCount)
    }


    private fun insertNode(
        dbCollection: RCollection,
        generatedCollection: Collection,
        childrenBeforeDeletion: TreeSet<CollectionTreeNode>? = null
    ) {
        val dbCollectionParentKey = dbCollection.parentKey
        val prevChildrenOrEmpty =
            childrenBeforeDeletion ?: TreeSet<CollectionTreeNode>(CollectionTreeComparator())
        if (dbCollectionParentKey == null) {
            nodes.add(
                CollectionTreeNode(
                    parentKey = null,
                    collection = generatedCollection,
                    children = prevChildrenOrEmpty
                )
            )
            attemptToAddNodesWithNoParent()
            return
        }

        val newNode = CollectionTreeNode(
            parentKey = dbCollectionParentKey,
            collection = generatedCollection,
            children = prevChildrenOrEmpty
        )
        val parentNode = nodeWithIdentifier(CollectionIdentifier.collection(dbCollectionParentKey))
        if (parentNode == null) {
            nodesWithNoPathToParent.add(newNode)
        } else {
            parentNode.children.add(newNode)
            attemptToAddNodesWithNoParent()
        }
    }

    private fun attemptToAddNodesWithNoParent() {
        val iterator = nodesWithNoPathToParent.iterator()
        while(iterator.hasNext()) {
            val nodeWithNoPath = iterator.next()
            val parentNode = nodeWithIdentifier(CollectionIdentifier.collection(nodeWithNoPath.parentKey!!))
            if (parentNode != null) {
                parentNode.children.add(nodeWithNoPath)
                iterator.remove()
                attemptToAddNodesWithNoParent()
                break
            }
        }
    }

    private fun nodeWithIdentifier(identifier: CollectionIdentifier): CollectionTreeNode? {
        return firstNode(
            matching = { node -> node.collection.identifier == identifier },
            array = this.nodes
        )
    }

    private fun removeNode(identifier: CollectionIdentifier): CollectionTreeNode? {
        val nodeFoundOnFirstLevel = nodes.find { node -> node.collection.identifier == identifier }
        if (nodeFoundOnFirstLevel != null) {
            nodes.remove(nodeFoundOnFirstLevel)
            return nodeFoundOnFirstLevel
        }

        val parentNode = parentNode(identifier)
        val nodeToRemove = parentNode?.children?.firstOrNull {it.collection.identifier == identifier}
        parentNode?.children?.remove(nodeToRemove)
        return nodeToRemove
    }

    fun parentNode(identifier: CollectionIdentifier): CollectionTreeNode? {
        return firstNode(
            matching = { node -> node.children.any { it.collection.identifier == identifier } },
            array = this.nodes
        )
    }

    private fun firstNode(
        matching: (CollectionTreeNode) -> Boolean,
        array: Set<CollectionTreeNode>
    ): CollectionTreeNode? {
        val queue = LinkedList(array)
        while (!queue.isEmpty()) {
            val node = queue.removeFirst()

            if (matching(node)) {
                return node
            }
            queue.addAll(node.children)
        }
        return null
    }

    private fun sendChangedToUi(
        includeTreeChanges: Boolean = false,
        includeCollapsedChanges: Boolean = false
    ) {
        val treeChanges = if (includeTreeChanges) {
            createListOfCollectionItemsWithChildren().toPersistentList()
        } else {
            null
        }
        val collapsedChanges = if (includeCollapsedChanges) {
            collapsed.toPersistentMap()
        } else {
            null
        }

        collectionTreeControllerInterface.sendChangesToUi(
            listOfCollectionItemsWithChildren = treeChanges,
            collapsed = collapsedChanges,
        )
    }

    fun createListOfCollectionItemsWithChildren(): List<CollectionItemWithChildren> {
        return this.nodes.map { node ->
            createCollectionItemWithChildrenRecursively(node)
        }
    }

    private fun createCollectionItemWithChildrenRecursively(
        currentNode: CollectionTreeNode,
    ): CollectionItemWithChildren {
        val currentCollection = this.collections[currentNode.collection.identifier]!!
        val children: MutableList<CollectionItemWithChildren> = mutableListOf()
        for (n in currentNode.children) {
            children.add(createCollectionItemWithChildrenRecursively(currentNode = n))
        }
        return CollectionItemWithChildren(collection = currentCollection, children = children.toImmutableList())
    }

    fun setCollapsed(collapsed: Boolean, identifier: CollectionIdentifier) {
        this.collapsed[identifier] = collapsed
        ignoreNextDbModification = true
        sendChangedToUi(includeCollapsedChanges = true)
    }

    fun cancel() {
        coroutineScope.cancel()
    }

    fun getCollectionByCollectionId(identifier: CollectionIdentifier): Collection? {
        return collections[identifier]
    }

    fun expandCollectionsIfNeeded(selectedCollectionId: CollectionIdentifier) {
        val listOfParentsToExpand = traverseCollectionTreeForSelectedCollection(
            selectedCollectionId = selectedCollectionId,
            items = this.nodes,
            listOfParents = LinkedList()
        )
        expandIdentifiers(listOfParentsToExpand.second)
    }

    private fun traverseCollectionTreeForSelectedCollection(
        selectedCollectionId: CollectionIdentifier,
        items: TreeSet<CollectionTreeNode>,
        listOfParents: LinkedList<CollectionIdentifier>
    ): Pair<Boolean, List<CollectionIdentifier>> {
        for (item in items) {
            if (item.collection.identifier == selectedCollectionId) {
                return true to listOfParents
            }
            listOfParents.add(item.collection.identifier)
            val traverseResult = traverseCollectionTreeForSelectedCollection(
                selectedCollectionId = selectedCollectionId,
                items = item.children,
                listOfParents = listOfParents
            )
            if (traverseResult.first) {
                return traverseResult
            }
        }
        return false to emptyList()
    }

    private fun expandIdentifiers(identifiersToCollapse: List<CollectionIdentifier>) {
        identifiersToCollapse.forEach {
            this.collapsed[it] = false
        }
        sendChangedToUi(includeCollapsedChanges = true)
    }
}
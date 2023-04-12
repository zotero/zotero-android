package org.zotero.android.screens.collections

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.OrderedCollectionChangeSet
import io.realm.OrderedRealmCollectionChangeListener
import io.realm.RealmResults
import kotlinx.coroutines.launch
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.LCE2
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.database.DbWrapper
import org.zotero.android.database.objects.RCollection
import org.zotero.android.database.objects.RCustomLibraryType
import org.zotero.android.database.objects.RItem
import org.zotero.android.database.requests.ReadCollectionsDbRequest
import org.zotero.android.database.requests.ReadItemsDbRequest
import org.zotero.android.database.requests.ReadLibraryDbRequest
import org.zotero.android.files.FileStore
import org.zotero.android.screens.allitems.data.AllItemsArgs
import org.zotero.android.screens.allitems.data.ItemsSortType
import org.zotero.android.screens.collectionedit.data.CollectionEditArgs
import org.zotero.android.screens.collections.data.CollectionItemWithChildren
import org.zotero.android.screens.collections.data.CollectionTree
import org.zotero.android.screens.collections.data.CollectionTreeBuilder
import org.zotero.android.screens.collections.data.CollectionsArgs
import org.zotero.android.screens.collections.data.CollectionsError
import org.zotero.android.sync.Collection
import org.zotero.android.sync.CollectionIdentifier
import org.zotero.android.sync.Library
import org.zotero.android.sync.LibraryIdentifier
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@HiltViewModel
internal class CollectionsViewModel @Inject constructor(
    val defaults: Defaults,
    private val dbWrapper: DbWrapper,
    private val fileStore: FileStore,
    private val dispatchers: Dispatchers,
) : BaseViewModel2<CollectionsViewState, CollectionsViewEffect>(CollectionsViewState()) {

    var allItems: RealmResults<RItem>? = null
    var unfiledItems: RealmResults<RItem>? = null
    var trashItems: RealmResults<RItem>? = null
    var collections: RealmResults<RCollection>? = null

    var isTablet: Boolean = false

    fun init(isTablet: Boolean) = initOnce {
        this.isTablet = isTablet
        viewModelScope.launch {
            val args = ScreenArguments.collectionsArgs
            initViewState(args)
            loadData()
        }

    }

    private fun initViewState(args: CollectionsArgs) {
        updateState {
            copy(
                libraryId = args.libraryId,
                library = Library(
                    identifier = LibraryIdentifier.custom(RCustomLibraryType.myLibrary),
                    name = "",
                    metadataEditable = false,
                    filesEditable = false
                ),
                selectedCollectionId = args.selectedCollectionId,
                collectionTree = CollectionTree(
                    nodes = mutableListOf(),
                    collections = ConcurrentHashMap(),
                    collapsed = ConcurrentHashMap()
                )
            )
        }

    }

    private fun loadData() {
        val libraryId = viewState.libraryId
        val includeItemCounts = defaults.showCollectionItemCounts()

        try {
            dbWrapper.realmDbStorage.perform { coordinator ->
                val library =
                    coordinator.perform(request = ReadLibraryDbRequest(libraryId = libraryId))
                collections =
                    coordinator.perform(request = ReadCollectionsDbRequest(libraryId = libraryId))

                var allItemCount = 0
                var unfiledItemCount = 0
                var trashItemCount = 0

                if (includeItemCounts) {
                    allItems = coordinator.perform(
                        request = ReadItemsDbRequest(
                            collectionId = CollectionIdentifier.custom(
                                CollectionIdentifier.CustomType.all
                            ),
                            libraryId = libraryId,
                            defaults = defaults
                        )
                    )
                    allItemCount = allItems!!.size

                    unfiledItems = coordinator.perform(
                        request = ReadItemsDbRequest(
                            collectionId = CollectionIdentifier.custom(
                                CollectionIdentifier.CustomType.unfiled
                            ),
                            libraryId = libraryId,
                            defaults = defaults
                        )
                    )
                    unfiledItemCount = unfiledItems!!.size

                    trashItems = coordinator.perform(
                        request = ReadItemsDbRequest(
                            collectionId = CollectionIdentifier.custom(
                                CollectionIdentifier.CustomType.trash
                            ),
                            libraryId = libraryId,
                            defaults = defaults
                        )
                    )
                    trashItemCount = trashItems!!.size
                    observeItemCount(
                        results = allItems!!,
                        customType = CollectionIdentifier.CustomType.all
                    )
                    observeItemCount(
                        results = unfiledItems!!,
                        customType = CollectionIdentifier.CustomType.unfiled
                    )
                    observeItemCount(
                        results = trashItems!!,
                        customType = CollectionIdentifier.CustomType.trash
                    )
                }

                val collectionTree = CollectionTreeBuilder.collections(
                    rCollections = collections!!,
                    libraryId = libraryId,
                    includeItemCounts = includeItemCounts
                )
                collectionTree.insert(
                    collection = Collection.initWithCustomType(
                        type = CollectionIdentifier.CustomType.all,
                        itemCount = allItemCount
                    ), index = 0
                )
                collectionTree.append(
                    collection = Collection.initWithCustomType(
                        type = CollectionIdentifier.CustomType.unfiled,
                        itemCount = unfiledItemCount
                    )
                )
                collectionTree.append(
                    collection = Collection.initWithCustomType(
                        type = CollectionIdentifier.CustomType.trash,
                        itemCount = trashItemCount
                    )
                )

                collections?.addChangeListener(OrderedRealmCollectionChangeListener<RealmResults<RCollection>> { objects, changeSet ->
                    val state = changeSet.state
                    when (state) {
                        OrderedCollectionChangeSet.State.INITIAL -> {
                            //no-op
                        }
                        OrderedCollectionChangeSet.State.UPDATE -> {
                            update(collections = objects, includeItemCounts = includeItemCounts)
                        }
                        OrderedCollectionChangeSet.State.ERROR -> {
                            Timber.e(
                                changeSet.error,
                                "CollectionsViewModel: could not load results"
                            )
                        }
                    }
                })
                updateState {
                    copy(
                        library = library,
                        lce = LCE2.Content
                    )
                }
                updateCollectionTree(collectionTree)
            }
        } catch (error: Exception) {
            Timber.e(error, "CollectionsActionHandlers: can't load data")
            updateState {
                copy(error = CollectionsError.dataLoading)
            }
        }
    }

    private fun updateCollectionTree(collectionTree: CollectionTree) {
        updateState {
            copy(
                collectionTree = collectionTree,
                collectionItemsToDisplay = collectionTree.createSnapshot()
            )
        }
        expandCollectionsIfNeeded()
        triggerEffect(CollectionsViewEffect.ScreenRefresh)
    }

    private fun expandCollectionsIfNeeded() {
        if (!isTablet) {
            return
        }
        val listOfParentsToExpand = traverseCollectionTreeForSelectedCollection(
            items = viewState.collectionItemsToDisplay,
            listOfParents = listOf()
        )
        for (parent in listOfParentsToExpand.second) {
            viewState.collectionTree.set(false, parent)
        }
    }

    private fun traverseCollectionTreeForSelectedCollection(
        items: List<CollectionItemWithChildren>,
        listOfParents: List<CollectionIdentifier>
    ): Pair<Boolean, List<CollectionIdentifier>> {
        for(item in items) {
            if (item.collection.identifier == viewState.selectedCollectionId) {
                return true to listOfParents
            }
            val traverseResult = traverseCollectionTreeForSelectedCollection(
                items = item.children,
                listOfParents = listOfParents + item.collection.identifier
            )
            if (traverseResult.first) {
                return traverseResult
            }
        }
        return false to emptyList()
    }

    private fun observeItemCount(results: RealmResults<RItem>, customType: CollectionIdentifier.CustomType) {
        results.addChangeListener(OrderedRealmCollectionChangeListener<RealmResults<RItem>> { items, changeSet ->
            val state = changeSet.state
            when (state) {
                OrderedCollectionChangeSet.State.INITIAL -> {
                    //no-op
                }
                OrderedCollectionChangeSet.State.UPDATE ->  {
                    update(itemsCount = items.size, customType = customType)
                }
                OrderedCollectionChangeSet.State.ERROR -> {
                    Timber.e(changeSet.error, "CollectionsViewModel: could not load results")
                }
            }
        })
    }

    private fun update(itemsCount: Int, customType: CollectionIdentifier.CustomType) {
        val collectionTree = viewState.collectionTree
        collectionTree.update(
            collection = Collection.initWithCustomType(
                type = customType,
                itemCount = itemsCount
            )
        )
        updateCollectionTree(collectionTree)
    }

    private fun update(collections: RealmResults<RCollection>, includeItemCounts: Boolean) {
        val tree = CollectionTreeBuilder.collections(collections, libraryId = viewState.libraryId, includeItemCounts = includeItemCounts)
        val collectionTree = viewState.collectionTree
        collectionTree.replace(matching = { it.isCollection }, tree = tree)
        updateCollectionTree(collectionTree)

        if (viewState.collectionTree.collection(viewState.selectedCollectionId) == null) {
            updateState {
                copy(selectedCollectionId = CollectionIdentifier.custom(CollectionIdentifier.CustomType.all))
            }
        }
        triggerEffect(CollectionsViewEffect.ScreenRefresh)
    }

    fun onItemTapped(collection: Collection) {
        updateState {
            copy(selectedCollectionId = collection.identifier)
        }
        fileStore.setSelectedCollectionId(collection.identifier)
        ScreenArguments.allItemsArgs = AllItemsArgs(
            collection = collection,
            library = viewState.library,
            sortType = ItemsSortType.default,
            searchTerm = null,
            error = null
        )
        triggerEffect(CollectionsViewEffect.NavigateToAllItemsScreen)
    }

    fun onItemChevronTapped(collection: Collection) {
        val tree = viewState.collectionTree
        tree.set(
            collapsed = !viewState.collectionTree.collapsed[collection.identifier]!!, collection.identifier
        )
        updateState {
            copy(collectionTree = tree)
        }
        triggerEffect(CollectionsViewEffect.ScreenRefresh)
    }

    fun onItemLongTapped(collection: Collection) {

    }

    override fun onCleared() {
        allItems?.removeAllChangeListeners()
        unfiledItems?.removeAllChangeListeners()
        trashItems?.removeAllChangeListeners()
        collections?.removeAllChangeListeners()
        super.onCleared()
    }

    fun onAdd() {
        ScreenArguments.collectionEditArgs = CollectionEditArgs(
            library = viewState.library,
            key = null,
            name = "",
            parent = null,
        )
        triggerEffect(CollectionsViewEffect.ShowCollectionEditEffect)
    }

}

internal data class  CollectionsViewState(
    val libraryId: LibraryIdentifier = LibraryIdentifier.group(0),
    val library: Library = Library(
        identifier = LibraryIdentifier.group(0),
        name = "",
        metadataEditable = false,
        filesEditable = false
    ),
    val collectionTree: CollectionTree = CollectionTree(
        mutableListOf(),
        ConcurrentHashMap(),
        ConcurrentHashMap()
    ),
    val collectionItemsToDisplay: List<CollectionItemWithChildren> = emptyList(),
    val selectedCollectionId: CollectionIdentifier = CollectionIdentifier.custom(
        CollectionIdentifier.CustomType.all
    ),
    val editingData: Triple<String?, String, Collection?>? = null,
    val error: CollectionsError? = null,
    val lce: LCE2 = LCE2.Loading,
) : ViewState {
    fun isCollapsed(snapshot: CollectionItemWithChildren): Boolean {
        return collectionTree.collapsed[snapshot.collection.identifier]!!
    }
}

internal sealed class  CollectionsViewEffect : ViewEffect {
    object NavigateBack : CollectionsViewEffect()
    object NavigateToAllItemsScreen : CollectionsViewEffect()
    object ShowCollectionEditEffect: CollectionsViewEffect()
    object ScreenRefresh : CollectionsViewEffect()
}

package org.zotero.android.screens.share.sharecollectionpicker

import dagger.hilt.android.lifecycle.HiltViewModel
import org.greenrobot.eventbus.EventBus
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.database.DbWrapper
import org.zotero.android.database.requests.ReadAllCustomLibrariesDbRequest
import org.zotero.android.database.requests.ReadAllWritableGroupsDbRequest
import org.zotero.android.database.requests.ReadCollectionsDbRequest
import org.zotero.android.screens.collections.data.CollectionItemWithChildren
import org.zotero.android.screens.collections.data.CollectionTree
import org.zotero.android.screens.collections.data.CollectionTreeBuilder
import org.zotero.android.screens.share.sharecollectionpicker.data.ShareCollectionPickerResults
import org.zotero.android.sync.Collection
import org.zotero.android.sync.CollectionIdentifier
import org.zotero.android.sync.Library
import org.zotero.android.sync.LibraryIdentifier
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class ShareCollectionPickerViewModel @Inject constructor(
    private val dbWrapper: DbWrapper,
) : BaseViewModel2<ShareCollectionPickerViewState, ShareCollectionPickerViewEffect>(ShareCollectionPickerViewState()) {

    var isTablet: Boolean = true

    fun init() = initOnce {
        val args = ScreenArguments.shareCollectionPickerArgs
        updateState {
            copy(
                selectedCollectionId = args.selectedCollectionId,
                selectedLibraryId = args.selectedLibraryId,
            )
        }
        loadData()
    }

    private fun loadData() {
        try {
            dbWrapper.realmDbStorage.perform { coordinator ->
                val customLibraries = coordinator.perform(ReadAllCustomLibrariesDbRequest())
                val groups = coordinator.perform(ReadAllWritableGroupsDbRequest())
                val libraries = customLibraries.map { Library(it) } + groups.map { Library(it) }
                val librariesCollapsed = mutableMapOf<LibraryIdentifier, Boolean>()
                val trees = mutableMapOf<LibraryIdentifier, CollectionTree>()
                for (library in libraries) {
                    val collections =
                        coordinator.perform(ReadCollectionsDbRequest(libraryId = library.identifier))
                    val tree = CollectionTreeBuilder.collections(
                        rCollections = collections,
                        libraryId = library.identifier,
                        includeItemCounts = false
                    )
                    tree.sortNodes()
                    tree.collapseAllCollections()

                    trees[library.identifier] = tree
                    librariesCollapsed[library.identifier] =
                        viewState.selectedLibraryId != library.identifier
                }
                updateState {
                    copy(
                        libraries = libraries,
                        librariesCollapsed = librariesCollapsed,
                    )
                }
                updateTreesToDisplay(trees)

            }

        } catch (error: Throwable) {
            Timber.e(error, "ShareCollectionPickerViewModel: can't load collections")
        }
    }


    fun onItemTapped(library: Library, collection: Collection?) {
        EventBus.getDefault().post(ShareCollectionPickerResults(collection, library))
        triggerEffect(ShareCollectionPickerViewEffect.OnBack)
    }

    private fun updateTreesToDisplay(trees: Map<LibraryIdentifier, CollectionTree>) {
        val treesToDisplay = trees.mapValues {
            it.value.createSnapshot()
        }
        for ((key, value) in treesToDisplay) {
            expandCollectionsIfNeeded(value, trees[key]!!)
        }
        updateState {
            copy(
                trees = trees,
                treesToDisplay = treesToDisplay
            )
        }
        triggerEffect(ShareCollectionPickerViewEffect.ScreenRefresh)
    }

    private fun expandCollectionsIfNeeded(
        collectionItemsToDisplay: List<CollectionItemWithChildren>,
        collectionTree: CollectionTree
    ) {
        val listOfParentsToExpand = traverseCollectionTreeForSelectedCollection(
            items = collectionItemsToDisplay,
            listOfParents = listOf()
        )
        for (parent in listOfParentsToExpand.second) {
            collectionTree.set(false, parent)
        }
    }

    private fun traverseCollectionTreeForSelectedCollection(
        items: List<CollectionItemWithChildren>,
        listOfParents: List<CollectionIdentifier>
    ): Pair<Boolean, List<CollectionIdentifier>> {
        for (item in items) {
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

    override fun onCleared() {
        super.onCleared()
    }

    fun onCollectionChevronTapped(libraryIdentifier: LibraryIdentifier, collection: Collection) {
        val tree = viewState.trees[libraryIdentifier]
        val collapsed = tree?.collapsed?.get(collection.identifier) ?: return
        tree.set(
            collapsed = !collapsed, collection.identifier
        )
        triggerEffect(ShareCollectionPickerViewEffect.ScreenRefresh)
    }

    fun onLibraryChevronTapped(libraryId: LibraryIdentifier) {
        val mutable = viewState.librariesCollapsed.toMutableMap()
        mutable[libraryId] = !(viewState.librariesCollapsed[libraryId] ?: true)
        updateState {
            copy(librariesCollapsed = mutable)
        }
        triggerEffect(ShareCollectionPickerViewEffect.ScreenRefresh)
    }

}

internal data class ShareCollectionPickerViewState(
    val selectedCollectionId: CollectionIdentifier = CollectionIdentifier.collection(""),
    val selectedLibraryId: LibraryIdentifier = LibraryIdentifier.group(0),

    val libraries: List<Library> = emptyList(),
    val librariesCollapsed: Map<LibraryIdentifier, Boolean> = mapOf(),

    val trees: Map<LibraryIdentifier, CollectionTree> = emptyMap(),
    val treesToDisplay: Map<LibraryIdentifier, List<CollectionItemWithChildren>> = emptyMap(),
) : ViewState {
    fun isCollapsed(libraryIdentifier: LibraryIdentifier, snapshot: CollectionItemWithChildren): Boolean {
        return trees[libraryIdentifier]?.collapsed?.get(snapshot.collection.identifier) ?: true
    }
}

internal sealed class ShareCollectionPickerViewEffect : ViewEffect {
    object OnBack : ShareCollectionPickerViewEffect()
    object ScreenRefresh : ShareCollectionPickerViewEffect()
}

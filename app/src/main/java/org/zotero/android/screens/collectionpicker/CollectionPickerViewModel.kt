package org.zotero.android.screens.collectionpicker

import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.OrderedCollectionChangeSet
import io.realm.OrderedRealmCollectionChangeListener
import io.realm.RealmResults
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import org.greenrobot.eventbus.EventBus
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.architecture.emptyImmutableSet
import org.zotero.android.database.DbWrapperMain
import org.zotero.android.database.objects.RCollection
import org.zotero.android.database.requests.ReadCollectionsDbRequest
import org.zotero.android.screens.collectionpicker.data.CollectionPickerMode
import org.zotero.android.screens.collectionpicker.data.CollectionPickerMultiResult
import org.zotero.android.screens.collectionpicker.data.CollectionPickerSingleResult
import org.zotero.android.screens.collections.data.CollectionItemWithChildren
import org.zotero.android.screens.collections.data.CollectionTree
import org.zotero.android.screens.collections.data.CollectionTreeBuilder
import org.zotero.android.sync.Collection
import org.zotero.android.sync.CollectionIdentifier
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.uicomponents.Plurals
import org.zotero.android.uicomponents.Strings
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class CollectionPickerViewModel @Inject constructor(
    private val dbWrapperMain: DbWrapperMain,
    private val context: Context,
) : BaseViewModel2<CollectionPickerViewState, CollectionPickerViewEffect>(CollectionPickerViewState()) {

    private lateinit var titleType: TitleType
    var multipleSelectionAllowed: Boolean = false
    private lateinit var results: RealmResults<RCollection>

    private var excludedKeys: Set<String> = emptySet()
    var libraryId: LibraryIdentifier = LibraryIdentifier.group(0)

    fun init() = initOnce {
        val args = ScreenArguments.collectionPickerArgs
        this.excludedKeys = args.excludedKeys
        this.libraryId = args.libraryId
        updateState {
            copy(
                selected = args.selected.toImmutableSet()
            )
        }

        when (args.mode) {
            is CollectionPickerMode.single -> {
                this.multipleSelectionAllowed = false
                this.titleType = TitleType.fixed(args.mode.title)
            }
            CollectionPickerMode.multiple -> {
                this.multipleSelectionAllowed = true
                this.titleType = TitleType.dynamicS
            }
        }

        val titleTypeLocal = this.titleType
        when(titleTypeLocal) {
            is TitleType.fixed -> {
                updateState {
                    copy(title = titleTypeLocal.str)
                }

            }
            is TitleType.dynamicS -> {
                updateTitle(viewState.selected.size)
            }
        }
        loadData()

    }

    private fun loadData() {
        try {
            val libraryId = this.libraryId
            val collectionsRequest = ReadCollectionsDbRequest(
                libraryId = libraryId,
                excludedKeys = this.excludedKeys
            )
            this.results = dbWrapperMain.realmDbStorage.perform(request = collectionsRequest)
            val collectionTree = CollectionTreeBuilder.collections(
                this.results,
                libraryId = libraryId,
                includeItemCounts = false
            )
            collectionTree.sortNodes()
            startObservingResults()
            updateCollectionTree(collectionTree)
        } catch (error: Throwable) {
            Timber.e(error, "CollectionPickerViewModel: can't load collections")
        }
    }

    private fun startObservingResults() {
        results.addChangeListener(OrderedRealmCollectionChangeListener<RealmResults<RCollection>> { collections, changeSet ->
            when (changeSet.state) {
                OrderedCollectionChangeSet.State.INITIAL -> {
                    //no-op
                }
                OrderedCollectionChangeSet.State.UPDATE -> {
                    update(collections)
                }
                OrderedCollectionChangeSet.State.ERROR -> {
                    Timber.e(changeSet.error, "ItemsViewController: could not load results")
                }
                else -> {
                    //no-op
                }
            }
        })
    }

    private fun update(results: RealmResults<RCollection>) {
        val tree = CollectionTreeBuilder.collections(
            results,
            libraryId = this.libraryId,
            includeItemCounts = false
        )
        tree.sortNodes()
        updateCollectionTree(tree)
        val removed = mutableSetOf<String>()
        for (key in viewState.selected) {
            if (tree.collection(CollectionIdentifier.collection(key)) != null) {
                continue
            }
            removed.add(key)
        }

        if (removed.isNotEmpty()) {
            updateState {
                copy(selected = viewState.selected.subtract(removed).toImmutableSet())
            }
            updateTitle(viewState.selected.size)
        }
    }


    private fun updateTitle(selectedCount: Int) {
        val title = if (selectedCount == 0) {
            context.resources.getString(Strings.collection_picker_select_collection)
        } else {
            context.resources.getQuantityString(
                Plurals.items_collections_selected,
                selectedCount,
                selectedCount
            )
        }
        updateState {
            copy(title = title)
        }
    }


    fun selectOrDeselect(collection: Collection) {
        val key = collection.identifier.keyGet!!
        if (viewState.selected.contains(key)) {
            deselect(key)
        } else {
            select(key)
        }
        if (!multipleSelectionAllowed) {
            EventBus.getDefault().post(CollectionPickerSingleResult(collection))
            triggerEffect(CollectionPickerViewEffect.OnBack)
        } else {
            updateTitle(viewState.selected.size)
        }
    }

    private fun select(key: String) {
        updateState {
            copy(selected = (selected + key).toImmutableSet())
        }
    }

    private fun deselect(key: String) {
        updateState {
            copy(selected = (selected - key).toImmutableSet())
        }
    }

    private fun updateCollectionTree(collectionTree: CollectionTree) {
        collectionTree.expandAllCollections()
        updateState {
            copy(
                collectionItemsToDisplay = collectionTree.createSnapshot().toImmutableList()
            )
        }
        triggerEffect(CollectionPickerViewEffect.ScreenRefresh)
    }

    fun confirmSelection() {
        EventBus.getDefault().post(CollectionPickerMultiResult(viewState.selected))
        triggerEffect(CollectionPickerViewEffect.OnBack)
    }

    override fun onCleared() {
        results.removeAllChangeListeners()
        super.onCleared()
    }

}

internal data class CollectionPickerViewState(
    val collectionItemsToDisplay: ImmutableList<CollectionItemWithChildren> = persistentListOf(),
    val selected: ImmutableSet<String> = emptyImmutableSet(),
    val title: String = ""
) : ViewState

internal sealed class CollectionPickerViewEffect : ViewEffect {
    object OnBack : CollectionPickerViewEffect()
    object ScreenRefresh : CollectionPickerViewEffect()
}

private sealed interface TitleType {
    data class fixed(val str: String): TitleType
    object dynamicS: TitleType

    val isDynamic: Boolean get() {
        return when (this) {
            is dynamicS -> true
            is fixed -> false
        }
    }
}
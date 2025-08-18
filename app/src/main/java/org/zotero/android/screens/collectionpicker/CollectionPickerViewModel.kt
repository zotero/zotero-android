package org.zotero.android.screens.collectionpicker

import android.content.Context
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.OrderedCollectionChangeSet
import io.realm.RealmResults
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.launch
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
import org.zotero.android.screens.collections.controller.CollectionTreeController
import org.zotero.android.screens.collections.controller.CollectionTreeControllerInterface
import org.zotero.android.screens.collections.data.CollectionItemWithChildren
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
    private val collectionTreeController: CollectionTreeController,
) : BaseViewModel2<CollectionPickerViewState, CollectionPickerViewEffect>(CollectionPickerViewState()),
    CollectionTreeControllerInterface {

    private lateinit var titleType: TitleType
    private var results: RealmResults<RCollection>? = null

    private var excludedKeys: Set<String> = emptySet()
    private var libraryId: LibraryIdentifier = LibraryIdentifier.group(0)

    fun init(isTablet: Boolean) = initOnce {
        initViewState()
        collectionTreeController.init(
            libraryId = this.libraryId,
            includeItemCounts = false,
            isTablet = isTablet,
            collectionTreeControllerInterface = this
        )
        initRequestAndStartObservingCollectionResults()
    }

    private fun initViewState() {
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
                updateState {
                    copy(multipleSelectionAllowed = false)
                }
                this.titleType = TitleType.fixed(args.mode.title)
            }

            CollectionPickerMode.multiple -> {
                updateState {
                    copy(multipleSelectionAllowed = true)
                }
                this.titleType = TitleType.dynamicS
            }
        }

        val titleTypeLocal = this.titleType
        when (titleTypeLocal) {
            is TitleType.fixed -> {
                updateState {
                    copy(title = titleTypeLocal.str)
                }
            }

            is TitleType.dynamicS -> {
                updateTitle(viewState.selected.size)
            }
        }
    }

    private fun initRequestAndStartObservingCollectionResults() {
        results = dbWrapperMain.realmDbStorage.perform(
            ReadCollectionsDbRequest(
                libraryId = libraryId,
                excludedKeys = this.excludedKeys,
                isAsync = true
            )
        )
        results?.addChangeListener { objects, changeSet ->
            when (changeSet.state) {
                OrderedCollectionChangeSet.State.INITIAL -> {
                    collectionTreeController.reactToCollectionsDbUpdate(
                        collections = objects,
                        changeSet = changeSet,
                    )
                }

                OrderedCollectionChangeSet.State.UPDATE -> {
                    collectionTreeController.reactToCollectionsDbUpdate(
                        collections = objects,
                        changeSet = changeSet,
                    )
                }

                OrderedCollectionChangeSet.State.ERROR -> {
                    Timber.e(
                        changeSet.error,
                        "CollectionPickerViewModel: could not load results"
                    )
                }
            }
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
        if (!viewState.multipleSelectionAllowed) {
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

    fun confirmSelection() {
        EventBus.getDefault().post(CollectionPickerMultiResult(viewState.selected))
        triggerEffect(CollectionPickerViewEffect.OnBack)
    }

    override fun sendChangesToUi(
        updatedItemsWithChildren: PersistentList<CollectionItemWithChildren>?,
        updatedCollapsed: PersistentMap<CollectionIdentifier, Boolean>?
    ) {
        viewModelScope.launch {
            updateState {
                copy(
                    collectionItemsToDisplay = updatedItemsWithChildren
                        ?: viewState.collectionItemsToDisplay,
                )
            }
            removeDeletedCollectionsFromSelection()
        }
    }

    override fun onItemTapped(collection: Collection) {
        //no-op
    }

    private fun removeDeletedCollectionsFromSelection() {
        val removed = mutableSetOf<String>()
        for (key in viewState.selected) {
            if (collectionTreeController.getCollectionByCollectionId(CollectionIdentifier.collection(key)) != null) {
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

    override fun onCleared() {
        results?.removeAllChangeListeners()
        super.onCleared()
    }

}

internal data class CollectionPickerViewState(
    val collectionItemsToDisplay: ImmutableList<CollectionItemWithChildren> = persistentListOf(),
    val selected: ImmutableSet<String> = emptyImmutableSet(),
    val title: String = "",
    val multipleSelectionAllowed: Boolean = false
) : ViewState

internal sealed class CollectionPickerViewEffect : ViewEffect {
    object OnBack : CollectionPickerViewEffect()
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
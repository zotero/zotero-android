package org.zotero.android.dashboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.ObjectChangeSet
import io.realm.RealmObjectChangeListener
import kotlinx.coroutines.launch
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.architecture.database.DbWrapper
import org.zotero.android.architecture.database.objects.Attachment
import org.zotero.android.architecture.database.objects.FieldKeys
import org.zotero.android.architecture.database.objects.RItem
import org.zotero.android.architecture.database.objects.UpdatableChangeType
import org.zotero.android.architecture.database.requests.ReadItemDbRequest
import org.zotero.android.dashboard.data.DetailType
import org.zotero.android.dashboard.data.ItemDetailData
import org.zotero.android.dashboard.data.ItemDetailError
import org.zotero.android.dashboard.data.ShowItemDetailsArgs
import org.zotero.android.files.FileStore
import org.zotero.android.sync.ItemDetailDataCreator
import org.zotero.android.sync.KeyGenerator
import org.zotero.android.sync.Library
import org.zotero.android.sync.Note
import org.zotero.android.sync.SchemaController
import org.zotero.android.sync.Tag
import org.zotero.android.sync.UrlDetector
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class ItemDetailsViewModel @Inject constructor(
    stateHandle: SavedStateHandle,
    private val defaults: Defaults,
    private val dbWrapper: DbWrapper,
    private val fileStore: FileStore,
    private val urlDetector: UrlDetector,
    private val schemaController: SchemaController
) : BaseViewModel2<ItemDetailsViewState, ItemDetailsViewEffect>(ItemDetailsViewState()) {

    fun init() = initOnce {
        val args = ScreenArguments.showItemDetailsArgs

        initViewState(args)
        process(ItemDetailAction.reloadData)
    }

    private fun initViewState(args: ShowItemDetailsArgs) {
        val type = args.type
        val library = args.library
        val preScrolledChildKey = args.childKey
        val userId = defaults.getUserId()

        when (type) {
            is DetailType.preview -> {
                updateState {
                    copy(
                        key = type.key,
                        isEditing = false)
                }

            }
            is DetailType.creation, is DetailType.duplication -> {
                updateState {
                    copy(
                        key = KeyGenerator.newKey(),
                        isEditing = true,
                    )
                }

            }

        }
        updateState {
            copy(
                type = type,
                userId = userId,
                library = library,
                preScrolledChildKey = preScrolledChildKey)
        }

    }

    private fun process(action: ItemDetailAction) {
        when (action) {
            ItemDetailAction.loadInitialData -> loadInitialData()
            ItemDetailAction.reloadData -> reloadData(viewState.isEditing)
        }
    }

    private fun loadInitialData() {
        val key = viewState.key
        val libraryId = viewState.library!!.identifier
        var collectionKey: String?

        try {
            when (viewState.type) {
                is DetailType.preview -> {
                    reloadData(isEditing = viewState.isEditing)
                    return
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "can't load initial data ")
            updateState {
                copy(error = ItemDetailError.cantCreateData)
            }
            return
        }
    }

    private fun reloadData(isEditing: Boolean) = viewModelScope.launch {
        try {
            val item = dbWrapper.realmDbStorage.perform(request = ReadItemDbRequest(libraryId = viewState.library!!.identifier, key = viewState.key), refreshRealm = true)

            item.addChangeListener(RealmObjectChangeListener<RItem> { items, changeSet ->
                if(changeSet?.changedFields?.any { RItem.observableKeypathsForItemDetail.contains(it) } == true) {
                    itemChanged(items, changeSet)
                }
                }
            )

            var (data, attachments, notes, tags) = ItemDetailDataCreator.createData(
                ItemDetailDataCreator.Kind.existing(item = item, ignoreChildren = false),
                schemaController = this@ItemDetailsViewModel.schemaController,
                fileStorage = this@ItemDetailsViewModel.fileStore, urlDetector = this@ItemDetailsViewModel.urlDetector,
                doiDetector = {doiValue -> FieldKeys.Item.isDoi(doiValue)})

            if (!isEditing) {
                data.fieldIds = ItemDetailDataCreator.filteredFieldKeys(data.fieldIds, fields = data.fields)
            }

            saveReloaded(data = data, attachments = attachments, notes = notes, tags = tags, isEditing = isEditing)
        } catch (e: Exception) {
            Timber.e(e,"ItemDetailActionHandler: can't load data")
            updateState {
                copy(error = ItemDetailError.cantCreateData)
            }
        }
    }

    private fun saveReloaded(data: ItemDetailData, attachments: List<Attachment>, notes: List<Note>,
                             tags: List<Tag>, isEditing: Boolean) {
        updateState {
            copy(data = data)
        }

        if (viewState.snapshot != null || isEditing) {
            updateState {
                val updatedData = data.copy(fieldIds = ItemDetailDataCreator.filteredFieldKeys(data.fieldIds, fields = data.fields))
                copy(snapshot = updatedData)
            }
        }
        updateState {
            copy(
                attachments = attachments,
                notes = notes,
                tags = tags,
                isLoadingData = false,
                isEditing = isEditing,
            )
        }
           //Update UI and hide progress.
    }

    private fun itemChanged(item: RItem, changeSet: ObjectChangeSet) {
        if (changeSet.isDeleted) {
            return
        }

        if (shouldReloadData(item, changeSet.changedFields)) {
            itemChanged(viewState)
        }
    }

    private fun shouldReloadData(item: RItem, changes: Array<String>): Boolean {
        if (changes.contains("version")) {
            //Use old value?
            if (changes.contains("changeType") && item.changeType != UpdatableChangeType.user.name) {
                return true
            }
            return false
        }

        if (changes.contains("children")) {
            return true
        }
        return false
    }

    //Start of Changes
    private fun itemChanged(state: ItemDetailsViewState) {
        if (!state.isEditing) {
            process(ItemDetailAction.reloadData)
            return
        }
        //TODO show Data Reloaded dialog for editing case
    }
    //End Of Changes
}

internal data class ItemDetailsViewState(
    val key : String = "",
    val library : Library? = null,
    val userId : Long = 0,
    val type: DetailType? = null,
    val preScrolledChildKey: String? = null,
    val isEditing: Boolean = false,
    var error: ItemDetailError? = null,
    var data: ItemDetailData = ItemDetailData.empty,
    var snapshot: ItemDetailData? = null,
    var notes: List<Note> = emptyList(),
    var attachments: List<Attachment> = emptyList(),
    var tags: List<Tag> = emptyList(),
    var isLoadingData: Boolean = false,
) : ViewState

internal sealed class ItemDetailsViewEffect : ViewEffect {
}

sealed class ItemDetailAction {
    object loadInitialData: ItemDetailAction()
    object reloadData: ItemDetailAction()
}
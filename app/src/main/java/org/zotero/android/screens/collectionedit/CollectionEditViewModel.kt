package org.zotero.android.screens.collectionedit

import android.content.Context
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.architecture.ifFailure
import org.zotero.android.database.DbRequest
import org.zotero.android.database.DbWrapperMain
import org.zotero.android.database.objects.RCollection
import org.zotero.android.database.requests.CreateCollectionDbRequest
import org.zotero.android.database.requests.EditCollectionDbRequest
import org.zotero.android.database.requests.MarkCollectionAndItemsAsDeletedDbRequest
import org.zotero.android.database.requests.MarkObjectsAsDeletedDbRequest
import org.zotero.android.screens.collectionedit.data.CollectionEditError
import org.zotero.android.screens.collectionpicker.data.CollectionPickerArgs
import org.zotero.android.screens.collectionpicker.data.CollectionPickerMode
import org.zotero.android.screens.collectionpicker.data.CollectionPickerSingleResult
import org.zotero.android.sync.Collection
import org.zotero.android.sync.KeyGenerator
import org.zotero.android.sync.Library
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.conflictresolution.AskUserToDeleteOrRestoreCollection
import org.zotero.android.sync.conflictresolution.ConflictResolutionUseCase
import org.zotero.android.uicomponents.Strings
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class CollectionEditViewModel @Inject constructor(
    private val dbWrapperMain: DbWrapperMain,
    private val context: Context,
    private val conflictResolutionUseCase: ConflictResolutionUseCase
) : BaseViewModel2<CollectionEditViewState, CollectionEditViewEffect>(CollectionEditViewState()) {

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(result: CollectionPickerSingleResult) {
        updateState {
            copy(parent = result.collection)
        }
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: AskUserToDeleteOrRestoreCollection) {
        updateState {
            copy(error = CollectionEditError.askUserToDeleteOrRestoreCollection)
        }
    }

    fun init() = initOnce {
        EventBus.getDefault().register(this)
        val args = ScreenArguments.collectionEditArgs
        updateState {
            copy(
                library = args.library,
                key = args.key,
                name = args.name,
                parent = args.parent
            )
        }
        conflictResolutionUseCase.currentlyDisplayedCollectionLibraryIdentifier = viewState.library.identifier
        conflictResolutionUseCase.currentlyDisplayedCollectionKey = viewState.key
    }

    fun onSave() {
        if (!viewState.isValid) {
            return
        }
        val key = viewState.key
        if (key != null) {
            val request = EditCollectionDbRequest(
                libraryId = viewState.library.identifier,
                key = key,
                name = viewState.name,
                parentKey = viewState.parent?.identifier?.keyGet
            )
            perform(request = request)
        } else {
            val request = CreateCollectionDbRequest(
                libraryId = viewState.library.identifier,
                key = KeyGenerator.newKey(),
                name = viewState.name,
                parentKey = viewState.parent?.identifier?.keyGet
            )
            perform(request = request)
        }
    }

    fun onNameChanged(text: String) {
        updateState {
            copy(name = text)
        }
    }

    override fun onCleared() {
        EventBus.getDefault().unregister(this)
        conflictResolutionUseCase.currentlyDisplayedCollectionLibraryIdentifier = null
        conflictResolutionUseCase.currentlyDisplayedCollectionKey = null
        super.onCleared()

    }

    private fun perform(request: DbRequest) {
        viewModelScope.launch {
            perform(
                dbWrapper = dbWrapperMain,
                request = request
            ).ifFailure {
                Timber.e(it, "CollectionEditViewModel: couldn't perform request")
                updateState {
                    copy(error = CollectionEditError.saveFailed)
                }
                return@launch
            }
            triggerEffect(CollectionEditViewEffect.OnBack)
        }
    }

    fun delete() {
        val key = viewState.key ?: return
        val request = MarkObjectsAsDeletedDbRequest(
            clazz = RCollection::class,
            keys = listOf(key),
            libraryId = viewState.library.identifier
        )
        perform(request = request)
    }

    fun deleteWithItems() {
        val key = viewState.key ?: return
        val request = MarkCollectionAndItemsAsDeletedDbRequest(
            key = key,
            libraryId = viewState.library.identifier
        )
        perform(request = request)
    }

    fun onParentClicked() {
        val selected = viewState.parent?.identifier?.keyGet ?: viewState.library.name
        val excludedKeys = if (viewState.key == null) emptySet() else setOf(viewState.key!!)

        ScreenArguments.collectionPickerArgs = CollectionPickerArgs(
            mode = CollectionPickerMode.single(title = context.getString(Strings.collections_picker_title)),
            libraryId = viewState.library.identifier,
            excludedKeys = excludedKeys,
            selected = setOf(selected)
        )
        triggerEffect(CollectionEditViewEffect.NavigateToCollectionPickerScreen)
    }

    fun onDismissErrorDialog() {
        updateState {
            copy(
                error = null,
            )
        }
    }
    fun deleteOrRestoreCollection(isDelete: Boolean) {
        conflictResolutionUseCase.deleteOrRestoreCollection(isDelete = isDelete)
        if (isDelete) {
            triggerEffect(CollectionEditViewEffect.OnBack)
        }
    }
}

internal data class CollectionEditViewState(
    val library: Library = Library(
        identifier = LibraryIdentifier.group(0),
        name = "",
        metadataEditable = false,
        filesEditable = false
    ),
    val key: String? = null,
    var name: String = "",
    var parent: Collection? = null,
    var error: CollectionEditError? = null,
) : ViewState {
    val isValid: Boolean
        get() {
            return !name.isEmpty()
        }
}

internal sealed class CollectionEditViewEffect : ViewEffect {
    object OnBack : CollectionEditViewEffect()
    object NavigateToCollectionPickerScreen : CollectionEditViewEffect()
}
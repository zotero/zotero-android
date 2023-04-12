package org.zotero.android.screens.collectionedit

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.architecture.ifFailure
import org.zotero.android.database.DbRequest
import org.zotero.android.database.DbWrapper
import org.zotero.android.database.objects.RCollection
import org.zotero.android.database.requests.CreateCollectionDbRequest
import org.zotero.android.database.requests.EditCollectionDbRequest
import org.zotero.android.database.requests.MarkCollectionAndItemsAsDeletedDbRequest
import org.zotero.android.database.requests.MarkObjectsAsDeletedDbRequest
import org.zotero.android.screens.collectionedit.data.CollectionEditError
import org.zotero.android.sync.Collection
import org.zotero.android.sync.KeyGenerator
import org.zotero.android.sync.Library
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SchemaController
import org.zotero.android.uicomponents.singlepicker.SinglePickerItem
import org.zotero.android.uicomponents.singlepicker.SinglePickerResult
import org.zotero.android.uicomponents.singlepicker.SinglePickerState
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class CollectionEditViewModel @Inject constructor(
    private val defaults: Defaults,
    private val schemaController: SchemaController,
    private val dbWrapper: DbWrapper,
) : BaseViewModel2<CollectionEditViewState, CollectionEditViewEffect>(CollectionEditViewState()) {

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(singlePickerResult: SinglePickerResult) {
        if (singlePickerResult.callPoint == SinglePickerResult.CallPoint.CreatorEdit) {
            viewModelScope.launch {
//                onCreatorTypeSelected(singlePickerResult.id)
            }
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

    fun createSinglePickerState(
        itemType: String,
        selected: String,
    ): SinglePickerState {
        val creators = schemaController.creators(itemType) ?: emptyList()
        val items = creators.mapNotNull { creator ->
            val name = schemaController.localizedCreator(creator.creatorType)
            if (name == null) {
                return@mapNotNull null
            }
            SinglePickerItem(id = creator.creatorType, name = name)
        }
        val state = SinglePickerState(objects = items, selectedRow = selected)
        return state
    }

    override fun onCleared() {
        super.onCleared()
        EventBus.getDefault().unregister(this)
    }

    private fun perform(request: DbRequest) {
        viewModelScope.launch {
            perform(
                dbWrapper = dbWrapper,
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
            return name.isEmpty()
        }
}

internal sealed class CollectionEditViewEffect : ViewEffect {
    object OnBack : CollectionEditViewEffect()
    object NavigateToLibraryPickerScreen : CollectionEditViewEffect()
}
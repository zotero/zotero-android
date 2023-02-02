package org.zotero.android.dashboard

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
import org.zotero.android.architecture.database.DbWrapper
import org.zotero.android.dashboard.data.ItemDetailCreator
import org.zotero.android.dashboard.data.SinglePickerArgs
import org.zotero.android.dashboard.ui.SinglePickerItem
import org.zotero.android.dashboard.ui.SinglePickerState
import org.zotero.android.files.FileStore
import org.zotero.android.sync.SchemaController
import org.zotero.android.sync.UrlDetector
import javax.inject.Inject

@HiltViewModel
internal class CreatorEditViewModel @Inject constructor(
    private val defaults: Defaults,
    private val dbWrapper: DbWrapper,
    private val fileStore: FileStore,
    private val urlDetector: UrlDetector,
    private val schemaController: SchemaController
) : BaseViewModel2<CreatorEditViewState, CreatorEditViewEffect>(CreatorEditViewState()) {

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(singlePickerResult: SinglePickerResult) {
        viewModelScope.launch {
            onCreatorTypeSelected(singlePickerResult.id)
        }
    }

    fun init() = initOnce {
        EventBus.getDefault().register(this)
        val args = ScreenArguments.creatorEditArgs
        updateState {
            copy(itemType = args.itemType, creator = args.creator)
        }
    }

    fun onSave() {
        if (!viewState.isValid) {
            return
        }
        EventBus.getDefault().post(viewState.creator!!)
        triggerEffect(CreatorEditViewEffect.OnBack)
    }

    fun onLastNameChange(text: String) {
        updateState {
            copy(creator = viewState.creator!!.copy(lastName = text))
        }
    }

    fun onFirstNameChange(text: String) {
        updateState {
            copy(creator = viewState.creator!!.copy(firstName = text))
        }
    }

    fun onFullNameChange(text: String) {
        updateState {
            copy(creator = viewState.creator!!.copy(fullName = text))
        }
    }

    fun toggleNamePresentation() {
        val updatedCreator = viewState.creator!!.copy()
        updatedCreator.toggle()
        updatedCreator.change()
        defaults.setCreatorNamePresentation(updatedCreator.namePresentation)
        updateState {
            copy(creator = updatedCreator)
        }
    }

    fun onCreatorTypeSelected(selectedCreatorType: String) {
        val updatedCreator = viewState.creator!!.copy()
        updatedCreator.type = selectedCreatorType
        updatedCreator.localizedType =
            this.schemaController.localizedCreator(selectedCreatorType) ?: ""
        updateState {
            copy(creator = updatedCreator)
        }
    }

    fun onCreatorTypeClicked() {
        val pickerState = createSinglePickerState(
            itemType = viewState.itemType, selected = viewState.creator!!.type
        )
        ScreenArguments.singlePickerArgs =
            SinglePickerArgs(
                singlePickerState = pickerState,
                title = viewState.creator!!.localizedType
            )
        triggerEffect(CreatorEditViewEffect.NavigateToSinglePickerScreen)
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
}

internal data class CreatorEditViewState(
    val itemType: String = "",
    val creator: ItemDetailCreator? = null,
) : ViewState {
    val isValid: Boolean
        get() {
            when (creator?.namePresentation) {
                ItemDetailCreator.NamePresentation.full -> return !creator.fullName.trim()
                    .isEmpty()
                ItemDetailCreator.NamePresentation.separate -> return !creator.firstName.trim()
                    .isEmpty() && !creator.lastName.trim().isEmpty()
                else -> return false
            }
        }
}

internal sealed class CreatorEditViewEffect : ViewEffect {
    object OnBack: CreatorEditViewEffect()
    object NavigateToSinglePickerScreen: CreatorEditViewEffect()
}
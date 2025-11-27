package org.zotero.android.screens.creatoredit

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.screens.itemdetails.data.DeleteCreatorAction
import org.zotero.android.screens.itemdetails.data.ItemDetailCreator
import org.zotero.android.sync.SchemaController
import org.zotero.android.uicomponents.singlepicker.SinglePickerItem
import org.zotero.android.uicomponents.singlepicker.SinglePickerState
import javax.inject.Inject

@HiltViewModel
internal class CreatorEditViewModel @Inject constructor(
    private val defaults: Defaults,
    private val schemaController: SchemaController,
) : BaseViewModel2<CreatorEditViewState, CreatorEditViewEffect>(CreatorEditViewState()) {

    fun init() = initOnce {
        val args = ScreenArguments.creatorEditArgs
        updateState {
            copy(itemType = args.itemType, creator = args.creator, isEditing = args.isEditing)
        }
        focusFirstField()
    }

    private fun focusFirstField() {
        viewModelScope.launch {
            // delay is needed, otherwise the keyboard doesn't show
            delay(200)
            if (viewState.creator?.namePresentation == ItemDetailCreator.NamePresentation.separate) {
                triggerEffect(CreatorEditViewEffect.RequestFocus(FocusField.LastName))
            } else {
                triggerEffect(CreatorEditViewEffect.RequestFocus(FocusField.FullName))
            }
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
        focusFirstField()
    }

    fun onCreatorTypeSelected(selectedCreatorType: String) {
        val updatedCreator = viewState.creator!!.copy()
        updatedCreator.type = selectedCreatorType
        updatedCreator.localizedType =
            this.schemaController.localizedCreator(selectedCreatorType) ?: ""
        updatedCreator.primary =
            schemaController.creatorIsPrimary(selectedCreatorType, itemType = viewState.itemType)
        updateState {
            copy(creator = updatedCreator)
        }
        dismissChooserDialog()
    }

    fun onCreatorTypeClicked() {
        val pickerState = createSinglePickerState(
            itemType = viewState.itemType, selected = viewState.creator!!.type
        )
        updateState {
            copy(
                listOfCreatorTypes = pickerState.objects.toPersistentList(),
                selectedCreatorType = pickerState.selectedRow,
                showChooserDialog = true
            )
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

    fun showDeleteCreatorConfirmation() {
        updateState {
            copy(shouldShowDeleteConfirmation = true)
        }
    }

    fun onDismissDeleteConformation() {
        updateState {
            copy(shouldShowDeleteConfirmation = false)
        }
    }

    fun deleteCreator() {
        EventBus.getDefault().post(DeleteCreatorAction(viewState.creator!!.id))
        triggerEffect(CreatorEditViewEffect.OnBack)
    }

    fun dismissChooserDialog() {
        updateState {
            copy(
                showChooserDialog = false
            )
        }
    }

    fun showChooserDialog() {
        updateState {
            copy(
                showChooserDialog = true
            )
        }
    }
}

internal data class CreatorEditViewState(
    val itemType: String = "",
    val creator: ItemDetailCreator? = null,
    val isEditing: Boolean = false,
    val shouldShowDeleteConfirmation: Boolean = false,

    val listOfCreatorTypes: PersistentList<SinglePickerItem> = persistentListOf(),
    val showChooserDialog: Boolean = false,
    val selectedCreatorType: String = "",
) : ViewState {
    val isValid: Boolean
        get() {
            return when (creator?.namePresentation) {
                ItemDetailCreator.NamePresentation.full -> !creator.fullName.trim()
                    .isEmpty()

                ItemDetailCreator.NamePresentation.separate -> !creator.firstName.trim()
                    .isEmpty() && !creator.lastName.trim().isEmpty()

                else -> false
            }
        }
}

internal sealed class CreatorEditViewEffect : ViewEffect {
    object OnBack : CreatorEditViewEffect()
    object NavigateToSinglePickerScreen : CreatorEditViewEffect()
    data class RequestFocus(val field: FocusField) : CreatorEditViewEffect()
}

sealed class FocusField : ViewEffect {
    object LastName : FocusField()
    object FullName : FocusField()
}

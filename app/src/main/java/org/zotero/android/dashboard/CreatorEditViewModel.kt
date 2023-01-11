package org.zotero.android.dashboard

import dagger.hilt.android.lifecycle.HiltViewModel
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.architecture.database.DbWrapper
import org.zotero.android.dashboard.data.ItemDetailCreator
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

    fun init() = initOnce {
        val args = ScreenArguments.creatorEditArgs
        updateState {
            copy(itemType = args.itemType, creator = args.creator)
        }
    }

    fun onSave() {
        //TODO
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
        updateState {
            copy(creator = updatedCreator)
        }
    }
}

internal data class CreatorEditViewState(
    val itemType: String = "",
    val creator: ItemDetailCreator? = null,

) : ViewState {
    val isValid: Boolean
        get() {
            when (creator?.namePresentation) {
                ItemDetailCreator.NamePresentation.full -> return !creator!!.fullName.trim()
                    .isEmpty()
                ItemDetailCreator.NamePresentation.separate -> return !creator!!.firstName.trim()
                    .isEmpty() && !creator!!.lastName.trim().isEmpty()
                else -> return false
            }
        }
}

internal sealed class CreatorEditViewEffect : ViewEffect {
}
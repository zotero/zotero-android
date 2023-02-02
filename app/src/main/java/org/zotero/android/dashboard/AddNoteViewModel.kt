package org.zotero.android.dashboard

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.architecture.database.DbWrapper
import org.zotero.android.dashboard.data.AddOrEditNoteArgs
import org.zotero.android.dashboard.data.SaveNoteAction
import org.zotero.android.sync.SchemaController
import javax.inject.Inject

@HiltViewModel
internal class AddNoteViewModel @Inject constructor(
    private val dbWrapper: DbWrapper,
    private val schemaController: SchemaController,
    private val dispatcher: CoroutineDispatcher,
) : BaseViewModel2<AddNoteViewState, AddNoteViewEffect>(AddNoteViewState()) {

    fun init() = initOnce {
        viewModelScope.launch {
            val args = ScreenArguments.addOrEditNoteArgs
            updateState {
                copy(title = args.title, text = args.text)
            }
        }
    }

    fun onDoneClicked() {
        val args = ScreenArguments.addOrEditNoteArgs
        EventBus.getDefault().post(
            SaveNoteAction(
                text = viewState.text,
                tags = args.tags,
                key = args.key,
                isFromDashboard = args.isFromDashboard
            )
        )
        triggerEffect(AddNoteViewEffect.NavigateBack)
    }

    fun onBodyTextChange(text: String) {
        updateState {
            copy(
                text = text
            )
        }
    }

}

internal data class AddNoteViewState(
    val title: AddOrEditNoteArgs.TitleData? = null,
    val text: String = "",
) : ViewState

internal sealed class AddNoteViewEffect : ViewEffect {
    object NavigateBack: AddNoteViewEffect()
}
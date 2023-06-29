package org.zotero.android.screens.addnote

import android.webkit.WebMessage
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.screens.addnote.data.AddOrEditNoteArgs
import org.zotero.android.screens.addnote.data.SaveNoteAction
import org.zotero.android.screens.addnote.data.WebViewInitMessage
import org.zotero.android.screens.addnote.data.WebViewUpdateResponse
import javax.inject.Inject

@HiltViewModel
internal class AddNoteViewModel @Inject constructor(
    private val gson: Gson,
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

    fun generateInitWebMessage(): WebMessage? {
        val gson = gson.toJson(
            WebViewInitMessage(
                instanceId = 1,
                message = WebViewInitMessage.WebViewInitPayload(
                    action = "init",
                    value = viewState.text,
                    readOnly = false,
                )
            )
        )
        return WebMessage(gson)
    }

    fun processWebViewResponse(message: WebMessage) {
        val data = message.data
        if (data.contains("update")) {
            val parsedMessage = gson.fromJson(data, WebViewUpdateResponse::class.java)
            updateNoteText(parsedMessage.message.value)
        }
    }

    private fun updateNoteText(noteText: String) {
        updateState {
            copy(
                text = noteText
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
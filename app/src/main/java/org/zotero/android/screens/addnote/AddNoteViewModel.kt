package org.zotero.android.screens.addnote

import android.webkit.WebMessage
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.database.objects.RCustomLibraryType
import org.zotero.android.screens.addnote.data.AddOrEditNoteArgs
import org.zotero.android.screens.addnote.data.SaveNoteAction
import org.zotero.android.screens.addnote.data.WebViewInitMessage
import org.zotero.android.screens.addnote.data.WebViewUpdateResponse
import org.zotero.android.screens.tagpicker.data.TagPickerArgs
import org.zotero.android.screens.tagpicker.data.TagPickerResult
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.Tag
import javax.inject.Inject

@HiltViewModel
internal class AddNoteViewModel @Inject constructor(
    private val gson: Gson,
) : BaseViewModel2<AddNoteViewState, AddNoteViewEffect>(AddNoteViewState()) {

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(tagPickerResult: TagPickerResult) {
        if (tagPickerResult.callPoint == TagPickerResult.CallPoint.AddNote) {
            viewModelScope.launch {
                updateState {
                    copy(tags = tagPickerResult.tags)
                }
//                triggerEffect(AddNoteViewEffect.RefreshUI)
            }
        }
    }

    fun init() = initOnce {
        EventBus.getDefault().register(this)
        viewModelScope.launch {
            val args = ScreenArguments.addOrEditNoteArgs
            updateState {
                copy(
                    title = args.title,
                    text = args.text,
                    tags = args.tags
                )
            }
        }
    }

    fun onDoneClicked() {
        val args = ScreenArguments.addOrEditNoteArgs
        EventBus.getDefault().post(
            SaveNoteAction(
                text = viewState.text,
                tags = viewState.tags,
                key = args.key,
                isFromDashboard = args.isFromDashboard
            )
        )
        triggerEffect(AddNoteViewEffect.NavigateBack)
    }

    fun generateInitWebMessage(): WebMessage {
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

    fun onTagsClicked() {
        ScreenArguments.tagPickerArgs = TagPickerArgs(
            libraryId = LibraryIdentifier.custom(RCustomLibraryType.myLibrary),
            selectedTags = viewState.tags.map { it.name }.toSet(),
            tags = emptyList(),
            callPoint = TagPickerResult.CallPoint.AddNote,
        )

        triggerEffect(AddNoteViewEffect.NavigateToTagPickerScreen)
    }

    override fun onCleared() {
        EventBus.getDefault().unregister(this)
    }
}

internal data class AddNoteViewState(
    val title: AddOrEditNoteArgs.TitleData? = null,
    val text: String = "",
    var tags: List<Tag> = emptyList(),
) : ViewState {
    fun formattedTags(): String {
        return tags.joinToString(separator = ", ") { it.name }
    }
}

internal sealed class AddNoteViewEffect : ViewEffect {
    object NavigateBack: AddNoteViewEffect()
    object NavigateToTagPickerScreen: AddNoteViewEffect()
    object RefreshUI: AddNoteViewEffect()
}
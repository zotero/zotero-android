package org.zotero.android.screens.addnote

import android.webkit.WebMessage
import android.webkit.WebMessagePort
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.architecture.navigation.ARG_ADD_OR_EDIT_NOTE
import org.zotero.android.architecture.navigation.NavigationParamsMarshaller
import org.zotero.android.architecture.require
import org.zotero.android.database.DbWrapperMain
import org.zotero.android.database.objects.RCustomLibraryType
import org.zotero.android.database.requests.ReadNoteDbRequest
import org.zotero.android.screens.addnote.data.AddOrEditNoteArgs
import org.zotero.android.screens.addnote.data.SaveNoteAction
import org.zotero.android.screens.addnote.data.WebViewSendMessage
import org.zotero.android.screens.addnote.data.WebViewUpdateResponse
import org.zotero.android.screens.tagpicker.data.TagPickerArgs
import org.zotero.android.screens.tagpicker.data.TagPickerResult
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.Tag
import java.nio.charset.StandardCharsets
import javax.inject.Inject

@HiltViewModel
internal class AddNoteViewModel @Inject constructor(
    private val gson: Gson,
    private val navigationParamsMarshaller: NavigationParamsMarshaller,
    stateHandle: SavedStateHandle,
    private val dbWrapperMain: DbWrapperMain,
) : BaseViewModel2<AddNoteViewState, AddNoteViewEffect>(AddNoteViewState()) {

    private lateinit var port: WebMessagePort

    private var isSaveDuringExit: Boolean = false

    lateinit var initialText: String
    lateinit var initialTags: List<Tag>

    private val addOrEditNoteArgs: AddOrEditNoteArgs by lazy {
        val argsEncoded = stateHandle.get<String>(ARG_ADD_OR_EDIT_NOTE).require()
        navigationParamsMarshaller.decodeObjectFromBase64(
            encodedJson = argsEncoded,
            charset = StandardCharsets.UTF_8
        )
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(tagPickerResult: TagPickerResult) {
        if (tagPickerResult.callPoint == TagPickerResult.CallPoint.AddNote) {
            viewModelScope.launch {
                updateState {
                    copy(tags = tagPickerResult.tags.toPersistentList())
                }
            }
        }
    }

    fun init() = initOnce {
        EventBus.getDefault().register(this)

        dbWrapperMain.realmDbStorage.perform {coordinatorAction ->
            val note = ReadNoteDbRequest(addOrEditNoteArgs.key).process(coordinatorAction.realm)
            initialText = note?.text ?: ""
            initialTags = note?.tags ?: listOf()
            viewModelScope.launch {
                updateState {
                    copy(
                        title = this@AddNoteViewModel.addOrEditNoteArgs.title,
                        text = this@AddNoteViewModel.initialText,
                        tags = this@AddNoteViewModel.initialTags.toPersistentList()
                    )
                }
            }
        }
    }

    fun generateInitWebMessage(): WebMessage {
        val gson = gson.toJson(
            WebViewSendMessage(
                instanceId = 1,
                message = WebViewSendMessage.WebViewSendMessagePayload(
                    action = "init",
                    value = viewState.text,
                    readOnly = false,
                )
            )
        )
        return WebMessage(gson)
    }

    private fun generateForceSaveMessage(): WebMessage {
        val gson = gson.toJson(
            WebViewSendMessage(
                instanceId = 1,
                message = WebViewSendMessage.WebViewSendMessagePayload(
                    action = "forceSave",
                    value = "",
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
            if (parsedMessage.message.value != null) {
                updateNoteText(parsedMessage.message.value)
            }
            if (isSaveDuringExit) {
                saveAndExit()
            }
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

    fun setPort(port: WebMessagePort) {
        this.port = port
    }

    fun onDoneClicked() {
        this.isSaveDuringExit = true
        updateState {
            copy(backHandlerInterceptionEnabled = false)
        }
        //Port might not be initialized by this point if user were to return to AddNoteScreen after a while, when app was loaded off memory and then very quickly tapped back button.
        //We skip saving message in this case because note was saved before that, when user visited screen last time.
        if (this::port.isInitialized) {
            this.port.postMessage(generateForceSaveMessage())
        } else {
            triggerEffect(AddNoteViewEffect.NavigateBack)
        }
    }

    private fun saveAndExit() {
        val text = viewState.text
        if (initialText != text || initialTags != viewState.tags) {
            println()
            EventBus.getDefault().post(
                SaveNoteAction(
                    text = text,
                    tags = viewState.tags,
                    key = addOrEditNoteArgs.key,
                    isFromDashboard = addOrEditNoteArgs.isFromDashboard
                )
            )
        }
        triggerEffect(AddNoteViewEffect.NavigateBack)
    }
}

internal data class AddNoteViewState(
    val title: AddOrEditNoteArgs.TitleData? = null,
    val text: String = "",
    var tags: PersistentList<Tag> = persistentListOf(),
    val backHandlerInterceptionEnabled: Boolean = true,
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
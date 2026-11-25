package org.zotero.android.screens.addnote

import android.webkit.WebView
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.architecture.navigation.ARG_ADD_OR_EDIT_NOTE
import org.zotero.android.architecture.navigation.NavigationParamsMarshaller
import org.zotero.android.architecture.require
import org.zotero.android.database.DbWrapperMain
import org.zotero.android.database.objects.RCustomLibraryType
import org.zotero.android.database.requests.ReadNoteDbRequest
import org.zotero.android.loaders.noteeditor.NoteEditorLoader
import org.zotero.android.screens.addnote.data.AddOrEditNoteArgs
import org.zotero.android.screens.addnote.data.SaveNoteAction
import org.zotero.android.screens.addnote.web.NoteEditorMessage
import org.zotero.android.screens.addnote.web.NoteEditorStartOptions
import org.zotero.android.screens.addnote.web.NoteEditorWebViewHandler
import org.zotero.android.screens.tagpicker.data.TagPickerArgs
import org.zotero.android.screens.tagpicker.data.TagPickerResult
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.Tag
import timber.log.Timber
import java.nio.charset.StandardCharsets
import javax.inject.Inject

@HiltViewModel
internal class AddNoteViewModel @Inject constructor(
    private val navigationParamsMarshaller: NavigationParamsMarshaller,
    stateHandle: SavedStateHandle,
    private val dbWrapperMain: DbWrapperMain,
    private val dispatchers: Dispatchers,
    private val noteEditorLoader: NoteEditorLoader,
    private val noteEditorWebViewHandler: NoteEditorWebViewHandler,
) : BaseViewModel2<AddNoteViewState, AddNoteViewEffect>(AddNoteViewState()) {

    private val addOrEditNoteArgs: AddOrEditNoteArgs by lazy {
        val argsEncoded = stateHandle.get<String>(ARG_ADD_OR_EDIT_NOTE).require()
        navigationParamsMarshaller.decodeObjectFromBase64(
            encodedJson = argsEncoded,
            charset = StandardCharsets.UTF_8
        )
    }

    private var noteText: String = ""
    private var isNoteLoaded = false
    private var isEditorInitialized = false
    private var lastSavedText: String = ""
    private var lastSavedTags: List<Tag> = emptyList()

    private var webViewSaveDeferred: CompletableDeferred<Unit>? = null
    private val rendererGoneTimestamps = mutableListOf<Long>()

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

        viewModelScope.launch {
            val isInstalled = withContext(dispatchers.io) { noteEditorLoader.ensureInstalled() }
            if (!isInstalled) {
                Timber.e("AddNoteViewModel: note editor is not installed")
            }
            updateState { copy(isEditorInstalled = isInstalled) }
        }

        dbWrapperMain.realmDbStorage.perform { coordinatorAction ->
            val note = ReadNoteDbRequest(addOrEditNoteArgs.key).process(coordinatorAction.realm)
            val text = note?.text ?: ""
            val tags = note?.tags ?: listOf()
            viewModelScope.launch {
                noteText = text
                lastSavedText = text
                lastSavedTags = tags
                isNoteLoaded = true
                updateState {
                    copy(
                        title = addOrEditNoteArgs.title,
                        tags = tags.toPersistentList(),
                        readOnly = addOrEditNoteArgs.readOnly,
                    )
                }
                startEditorIfReady()
            }
        }
    }

    fun initWebView(webView: WebView) {
        isEditorInitialized = false
        noteEditorWebViewHandler.load(
            webView = webView,
            onEditorMessage = ::onEditorMessage,
            onRendererGone = ::onEditorRendererGone,
        )
    }

    fun onWebViewReleased(webView: WebView) {
        noteEditorWebViewHandler.release(webView)
    }

    private fun onEditorMessage(message: NoteEditorMessage) {
        when (message) {
            NoteEditorMessage.Initialized -> {
                isEditorInitialized = true
                startEditorIfReady()
            }

            NoteEditorMessage.EditorStarted -> Timber.i("AddNoteViewModel: editor started")

            is NoteEditorMessage.Update -> {
                message.value?.let {
                    noteText = it
                }
                webViewSaveDeferred?.complete(Unit)
            }

            is NoteEditorMessage.Log -> Timber.i("AddNoteViewModel JS: ${message.text}")
            is NoteEditorMessage.Unknown -> Timber.w("AddNoteViewModel: unknown editor message ${message.action}")
        }
    }

    private fun startEditorIfReady() {
        if (!isEditorInitialized || !isNoteLoaded) {
            return
        }
        noteEditorWebViewHandler.start(
            NoteEditorStartOptions(
                value = noteText,
                readOnly = addOrEditNoteArgs.readOnly,
                showIndentButtons = noteEditorWebViewHandler.showIndentButtons,
            )
        )
    }

    private fun onEditorRendererGone() {
        isEditorInitialized = false
        webViewSaveDeferred?.complete(Unit)

        val now = System.currentTimeMillis()
        rendererGoneTimestamps.removeAll { now - it > RENDERER_GONE_WINDOW_MS }
        rendererGoneTimestamps.add(now)
        if (rendererGoneTimestamps.size > MAX_RENDERER_RECOVERIES_IN_WINDOW) {
            triggerEffect(AddNoteViewEffect.NavigateBack)
            return
        }
        updateState {
            copy(webViewGeneration = webViewGeneration + 1)
        }
    }

    private fun saveNote() {
        if (lastSavedText != noteText || lastSavedTags != viewState.tags) {
            EventBus.getDefault().post(
                SaveNoteAction(
                    text = noteText,
                    tags = viewState.tags,
                    key = addOrEditNoteArgs.key,
                    isFromDashboard = addOrEditNoteArgs.isFromDashboard
                )
            )
        }
    }

    private suspend fun flushAndSave() {
        if (isEditorInitialized && noteEditorWebViewHandler.isAlive) {
            val deferred = CompletableDeferred<Unit>()
            webViewSaveDeferred = deferred
            noteEditorWebViewHandler.forceSave()
            withTimeoutOrNull(FORCE_SAVE_TIMEOUT_MS) { deferred.await() }
            webViewSaveDeferred = null
        }
        saveNote()
    }

    fun onStop() = viewModelScope.launch {
        if (!isNoteLoaded) {
            return@launch
        }
        flushAndSave()
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

    private companion object {
        const val FORCE_SAVE_TIMEOUT_MS = 1500L
        const val RENDERER_GONE_WINDOW_MS = 60_000L
        const val MAX_RENDERER_RECOVERIES_IN_WINDOW = 3
    }
}

internal data class AddNoteViewState(
    val title: AddOrEditNoteArgs.TitleData? = null,
    var tags: PersistentList<Tag> = persistentListOf(),
    val readOnly: Boolean = false,
    val isEditorInstalled: Boolean = false,
    val webViewGeneration: Int = 0,
) : ViewState {
    fun formattedTags(): String {
        return tags.joinToString(separator = ", ") { it.name }
    }
}

internal sealed class AddNoteViewEffect : ViewEffect {
    object NavigateBack : AddNoteViewEffect()
    object NavigateToTagPickerScreen : AddNoteViewEffect()
    object RefreshUI : AddNoteViewEffect()
}
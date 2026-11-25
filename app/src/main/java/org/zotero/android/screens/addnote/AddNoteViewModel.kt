package org.zotero.android.screens.addnote

import android.webkit.WebView
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
import org.zotero.android.database.objects.ItemTypes
import org.zotero.android.database.requests.CreateNoteDbRequest
import org.zotero.android.database.requests.EditNoteDbRequest
import org.zotero.android.database.requests.ReadNoteDbRequest
import org.zotero.android.loaders.noteeditor.NoteEditorLoader
import org.zotero.android.screens.addnote.data.AddOrEditNoteArgs
import org.zotero.android.screens.addnote.data.NoteSavedAction
import org.zotero.android.screens.addnote.web.NoteEditorMessage
import org.zotero.android.screens.addnote.web.NoteEditorStartOptions
import org.zotero.android.screens.addnote.web.NoteEditorWebViewHandler
import org.zotero.android.screens.tagpicker.data.TagPickerArgs
import org.zotero.android.screens.tagpicker.data.TagPickerResult
import org.zotero.android.sync.Note
import org.zotero.android.sync.SchemaController
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
    private val schemaController: SchemaController,
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

    @Volatile
    private var noteExistsInDb = false
    private var lastSavedText: String = ""
    private var lastSavedTags: List<Tag> = emptyList()

    private val saveMutex = Mutex()
    private var saveJob: Job? = null
    private var isDiscarding = false

    private var webViewSaveDeferred: CompletableDeferred<Unit>? = null
    private val rendererGoneTimestamps = mutableListOf<Long>()

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(tagPickerResult: TagPickerResult) {
        if (tagPickerResult.callPoint == TagPickerResult.CallPoint.AddNote) {
            viewModelScope.launch {
                updateState {
                    copy(tags = tagPickerResult.tags.toPersistentList())
                }
                scheduleSave()
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
                noteExistsInDb = note != null
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
                    scheduleSave()
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

    private fun scheduleSave() {
        if (addOrEditNoteArgs.readOnly) {
            return
        }
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(SAVE_DEBOUNCE_MS)
            if (!save()) {
                updateState { copy(error = AddNoteError(isClosing = false)) }
            }
        }
    }

    private suspend fun save(force: Boolean = false): Boolean = saveMutex.withLock {
        if (addOrEditNoteArgs.readOnly || !isNoteLoaded) {
            return@withLock true
        }
        val text = noteText
        val tags = viewState.tags.toList()
        val hasChanges = text != lastSavedText || tags != lastSavedTags
        val mustCreate = force && !noteExistsInDb
        if (!hasChanges && !mustCreate) {
            return@withLock true
        }

        val error = withContext(NonCancellable + dispatchers.io) {
            try {
                store(text = text, tags = tags)
                null
            } catch (e: Throwable) {
                e
            }
        }
        if (error != null) {
            Timber.e(error, "AddNoteViewModel: can't save note ${addOrEditNoteArgs.key}")
            return@withLock false
        }

        noteExistsInDb = true
        lastSavedText = text
        lastSavedTags = tags
        EventBus.getDefault().post(
            NoteSavedAction(
                key = addOrEditNoteArgs.key,
                text = text,
                tags = tags,
                parentKey = addOrEditNoteArgs.parentKey,
            )
        )
        true
    }

    private fun store(text: String, tags: List<Tag>) {
        val note = Note(key = addOrEditNoteArgs.key, text = text, tags = tags)
        val libraryId = addOrEditNoteArgs.library.identifier
        Timber.i("AddNoteViewModel: ${if (noteExistsInDb) "edit" else "create"} note ${note.key}; parent=${addOrEditNoteArgs.parentKey}; collection=${addOrEditNoteArgs.collectionKey}")
        if (noteExistsInDb) {
            dbWrapperMain.realmDbStorage.perform(EditNoteDbRequest(note = note, libraryId = libraryId))
            return
        }
        val request = CreateNoteDbRequest(
            note = note,
            localizedType = schemaController.localizedItemType(ItemTypes.note) ?: ItemTypes.note,
            libraryId = libraryId,
            collectionKey = addOrEditNoteArgs.collectionKey,
            parentKey = addOrEditNoteArgs.parentKey,
        )
        dbWrapperMain.realmDbStorage.perform(request = request, invalidateRealm = true)
    }

    private suspend fun flushAndSave(): Boolean {
        saveJob?.cancel()
        if (isEditorInitialized && noteEditorWebViewHandler.isAlive) {
            val deferred = CompletableDeferred<Unit>()
            webViewSaveDeferred = deferred
            noteEditorWebViewHandler.forceSave()
            withTimeoutOrNull(FORCE_SAVE_TIMEOUT_MS) { deferred.await() }
            webViewSaveDeferred = null
        }
        return save()
    }

    fun onBackClicked() {
        if (viewState.isClosing) {
            return
        }
        viewModelScope.launch {
            updateState { copy(isClosing = true) }
            if (flushAndSave()) {
                triggerEffect(AddNoteViewEffect.NavigateBack)
            } else {
                updateState { copy(isClosing = false, error = AddNoteError(isClosing = true)) }
            }
        }
    }

    fun onDismissError() {
        updateState { copy(error = null) }
    }

    fun onCloseWithoutSaving() {
        isDiscarding = true
        saveJob?.cancel()
        updateState { copy(error = null) }
        triggerEffect(AddNoteViewEffect.NavigateBack)
    }

    fun onStop() = viewModelScope.launch {
        if (!isNoteLoaded || isDiscarding) {
            return@launch
        }
        if (!flushAndSave()) {
            updateState { copy(error = AddNoteError(isClosing = false)) }
        }
    }

    fun onTagsClicked() {
        if (addOrEditNoteArgs.readOnly) {
            return
        }
        ScreenArguments.tagPickerArgs = TagPickerArgs(
            libraryId = addOrEditNoteArgs.library.identifier,
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
        const val SAVE_DEBOUNCE_MS = 1000L
        const val FORCE_SAVE_TIMEOUT_MS = 1500L
        const val RENDERER_GONE_WINDOW_MS = 60_000L
        const val MAX_RENDERER_RECOVERIES_IN_WINDOW = 3
    }
}

internal data class AddNoteError(val isClosing: Boolean)

internal data class AddNoteViewState(
    val title: AddOrEditNoteArgs.TitleData? = null,
    var tags: PersistentList<Tag> = persistentListOf(),
    val readOnly: Boolean = false,
    val isClosing: Boolean = false,
    val error: AddNoteError? = null,
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
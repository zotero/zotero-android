package org.zotero.android.dashboard

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.architecture.database.DbError
import org.zotero.android.architecture.database.DbWrapper
import org.zotero.android.architecture.database.objects.ItemTypes
import org.zotero.android.architecture.database.requests.CreateNoteDbRequest
import org.zotero.android.architecture.database.requests.EditNoteDbRequest
import org.zotero.android.dashboard.data.AddOrEditNoteArgs
import org.zotero.android.sync.CollectionIdentifier
import org.zotero.android.sync.Note
import org.zotero.android.sync.SchemaController
import org.zotero.android.sync.Tag
import timber.log.Timber
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
        viewModelScope.launch {
            saveNote(key = ScreenArguments.addOrEditNoteArgs.key, text = viewState.text,
                tags = ScreenArguments.addOrEditNoteArgs.tags)
            triggerEffect(AddNoteViewEffect.NavigateBack)
        }
    }

    private suspend fun saveNote(text: String, tags: List<Tag>, key: String) = withContext(dispatcher) {
        val note = Note(key = key, text = text, tags = tags)
        val libraryId = ScreenArguments.addOrEditNoteArgs.library.identifier
        var collectionKey: String? = null

        val identifier = ScreenArguments.addOrEditNoteArgs.collection.identifier
        when (identifier) {
            is CollectionIdentifier.collection ->
                collectionKey = identifier.key
            is CollectionIdentifier.custom, is CollectionIdentifier.search ->
                collectionKey = null
        }

        try {
            dbWrapper.realmDbStorage.perform(
                EditNoteDbRequest(
                    note = note,
                    libraryId = libraryId
                )
            )
        } catch (e: Throwable) {
            if (e is DbError.objectNotFound) {
                val request = CreateNoteDbRequest(
                    note = note, localizedType = (schemaController.localizedItemType(
                        ItemTypes.note
                    ) ?: ""), libraryId = libraryId, collectionKey = collectionKey, parentKey = null
                )
                dbWrapper.realmDbStorage.perform(request = request, invalidateRealm = true)
            } else {
                Timber.e(e)
            }
        }
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
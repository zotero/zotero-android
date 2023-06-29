package org.zotero.android.screens.tagpicker

import dagger.hilt.android.lifecycle.HiltViewModel
import org.greenrobot.eventbus.EventBus
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.database.DbWrapper
import org.zotero.android.database.requests.ReadTagPickerTagsDbRequest
import org.zotero.android.ktx.index
import org.zotero.android.screens.tagpicker.data.TagPickerResult
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.Tag
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class TagPickerViewModel @Inject constructor(
    private val dbWrapper: DbWrapper,
) : BaseViewModel2<TagPickerViewState, TagPickerViewEffect>(TagPickerViewState()) {

    fun init() = initOnce {
        val args = ScreenArguments.tagPickerArgs
        updateState {
            copy(libraryId = args.libraryId, selectedTags = args.selectedTags, tags = args.tags)
        }
        if (viewState.tags.isEmpty()) {
            load()
        }
    }

    fun selectOrDeselect(name: String) {
        if (viewState.selectedTags.contains(name)) {
            deselect(name)
        } else {
            select(name)
        }
    }

    private fun select(name: String) {
        updateState {
            copy(selectedTags = selectedTags + name)
        }
    }

    private fun deselect(name: String) {
        updateState {
            copy(selectedTags = selectedTags - name)
        }
    }

    private fun add(name: String) {
        val snapshot = viewState.snapshot ?: return
        val tag = Tag(name = name, color = "")
        updateState {
            copy(tags = snapshot)
        }

        val index = viewState.tags.index(
            tag,
            sortedBy = { first, second ->
                first.name.compareTo(
                    second.name,
                    ignoreCase = true
                ) == 1
            })

        val updatedTags = viewState.tags.toMutableList()
        updatedTags.add(index, tag)
        val updatedSelectedTags = viewState.selectedTags.toMutableSet()
        updatedSelectedTags.add(name)

        updateState {
            copy(
                searchTerm = "",
                tags = updatedTags,
                selectedTags = updatedSelectedTags,
                snapshot = null,
                addedTagName = name,
                showAddTagButton = false
            )
        }
    }


    fun search(term: String) {
        if (!term.isEmpty()) {
            if (viewState.snapshot == null) {
                updateState {
                    copy(snapshot = viewState.tags)
                }
            }
            var updatedTags = viewState.snapshot ?: viewState.tags
            updatedTags = updatedTags.filter { it.name.contains(term, ignoreCase = true) }
            updateState {
                copy(
                    searchTerm = term,
                    tags = updatedTags,
                    showAddTagButton = viewState.tags.isEmpty() || viewState.tags.firstOrNull { it.name == term } == null)
            }
        } else {
            val snapshot = viewState.snapshot ?: return
            updateState {
                copy(
                    searchTerm = "",
                    tags = snapshot,
                    snapshot = null,
                    showAddTagButton = false
                )
            }
        }
    }

    private fun load() {
        try {
            val request = ReadTagPickerTagsDbRequest(libraryId = viewState.libraryId!!)
            val results = dbWrapper.realmDbStorage.perform(request = request)
            val colored = results.where().notEqualTo("color", "\"\"").sort("name").findAll()
            val others = results.where().equalTo("color", "\"\"").sort("name").findAll()
            val tags = colored.map { Tag(it) } + others.map { Tag(it) }
            updateState {
                copy(tags = tags)
            }
        } catch (e: Exception) {
            Timber.e(e, "TagPicker: can't load tag")
        }
    }

    fun onSave() {
        val allTags = viewState.snapshot ?: viewState.tags
        val tags = viewState.selectedTags.mapNotNull { id ->
            allTags.firstOrNull { it.id == id }
        }.sortedBy { it.name }
        EventBus.getDefault().post(TagPickerResult(
            tags = tags,
            callPoint = ScreenArguments.tagPickerArgs.callPoint
        ))
        triggerEffect(TagPickerViewEffect.OnBack)
    }

    fun addTagIfNeeded() {
        val text = viewState.searchTerm.trim()
        if (text.isEmpty()) return
        add(text)
    }

}

internal data class TagPickerViewState(
    val libraryId: LibraryIdentifier? = null,
    val tags: List<Tag> = emptyList(),
    val snapshot: List<Tag>? = null,
    val selectedTags: Set<String> = emptySet(),
    val searchTerm: String = "",
    val showAddTagButton: Boolean = false,
    val addedTagName: String? = null
) : ViewState

internal sealed class TagPickerViewEffect : ViewEffect {
    object OnBack : TagPickerViewEffect()
}
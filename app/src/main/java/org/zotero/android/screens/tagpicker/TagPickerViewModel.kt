package org.zotero.android.screens.tagpicker

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import org.greenrobot.eventbus.EventBus
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.database.DbWrapperMain
import org.zotero.android.database.requests.ReadTagPickerTagsDbRequest
import org.zotero.android.ktx.index
import org.zotero.android.screens.tagpicker.data.TagPickerResult
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.Tag
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class TagPickerViewModel @Inject constructor(
    private val dbWrapperMain: DbWrapperMain,
) : BaseViewModel2<TagPickerViewState, TagPickerViewEffect>(TagPickerViewState()) {

    private var snapshot: List<Tag>? = null
    private var libraryId: LibraryIdentifier? = null

    fun init() = initOnce {
        val args = ScreenArguments.tagPickerArgs
        this.libraryId = args.libraryId
        updateState {
            copy(
                selectedTags = args.selectedTags.toPersistentSet(),
                tags = args.tags.toPersistentList()
            )
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
            copy(selectedTags = viewState.selectedTags.add(name))
        }
    }

    private fun deselect(name: String) {
        updateState {
            copy(selectedTags = viewState.selectedTags.remove(name))
        }
    }

    private fun add(name: String) {
        val snapshot = this.snapshot ?: return
        val tag = Tag(name = name, color = "")
        updateState {
            copy(tags = snapshot.toPersistentList())
        }

        val index = viewState.tags.index(
            tag,
            sortedBy = { first, second ->
                first.name.compareTo(
                    second.name,
                    ignoreCase = true
                ) == 1
            })


        this.snapshot = null
        updateState {
            copy(
                searchTerm = "",
                tags = viewState.tags.add(index, tag),
                selectedTags = viewState.selectedTags.add(name),
                showAddTagButton = false
            )
        }
    }


    fun search(term: String) {
        if (!term.isEmpty()) {
            if (this.snapshot == null) {
                this.snapshot = viewState.tags
            }
            var updatedTags = this.snapshot ?: viewState.tags
            updatedTags = updatedTags.filter { it.name.contains(term, ignoreCase = true) }
            updateState {
                copy(
                    searchTerm = term,
                    tags = updatedTags.toPersistentList(),
                    showAddTagButton = viewState.tags.isEmpty() || viewState.tags.firstOrNull { it.name == term } == null)
            }
        } else {
            val snapshot = this.snapshot ?: return
            this.snapshot = null
            updateState {
                copy(
                    searchTerm = "",
                    tags = snapshot.toPersistentList(),
                    showAddTagButton = false
                )
            }
        }
    }

    private fun load() {
        try {
            val request = ReadTagPickerTagsDbRequest(libraryId = this.libraryId!!)
            val results = dbWrapperMain.realmDbStorage.perform(request = request)
            val colored = results.where().isNotEmpty("color").sort("name").findAll()
            val others = results.where().isEmpty("color").sort("name").findAll()
            val tags = colored.map { Tag(it) } + others.map { Tag(it) }
            updateState {
                copy(tags = tags.toPersistentList())
            }
        } catch (e: Exception) {
            Timber.e(e, "TagPicker: can't load tag")
        }
    }

    fun onSave() {
        val allTags = this.snapshot ?: viewState.tags
        val tags = viewState.selectedTags.mapNotNull { id ->
            allTags.firstOrNull { it.id == id }
        }.sortedBy { it.name }
        EventBus.getDefault().post(
            TagPickerResult(
                tags = tags,
                callPoint = ScreenArguments.tagPickerArgs.callPoint
            )
        )
        triggerEffect(TagPickerViewEffect.OnBack)
    }

    fun addTagIfNeeded() {
        val text = viewState.searchTerm.trim()
        if (text.isEmpty()) return
        add(text)
    }

}

internal data class TagPickerViewState(
    val tags: PersistentList<Tag> = persistentListOf(),
    val selectedTags: PersistentSet<String> = persistentSetOf(),
    val searchTerm: String = "",
    val showAddTagButton: Boolean = false,
) : ViewState

internal sealed class TagPickerViewEffect : ViewEffect {
    object OnBack : TagPickerViewEffect()
}
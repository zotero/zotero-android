package org.zotero.android.screens.filter

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.database.DbWrapperMain
import org.zotero.android.database.requests.DeleteAutomaticTagsDbRequest
import org.zotero.android.database.requests.ReadAutomaticTagsDbRequest
import org.zotero.android.database.requests.ReadColoredTagsDbRequest
import org.zotero.android.database.requests.ReadFilteredTagsDbRequest
import org.zotero.android.screens.allitems.data.ItemsFilter
import org.zotero.android.screens.filter.data.FilterDialog
import org.zotero.android.screens.filter.data.FilterReloadEvent
import org.zotero.android.screens.filter.data.FilterResult
import org.zotero.android.screens.filter.data.FilterTag
import org.zotero.android.sync.CollectionIdentifier
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.Tag
import org.zotero.android.uicomponents.bottomsheet.LongPressOptionItem
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class FilterViewModel @Inject constructor(
    private val dbWrapperMain: DbWrapperMain,
    private val defaults: Defaults,
    private val dispatchers: Dispatchers,
) : BaseViewModel2<FilterViewState, FilterViewEffect>(FilterViewState()) {

    private val LIST_CHUNK_SIZE = 50

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: FilterReloadEvent) {
        itemsDidChange(event.filters, event.collectionId, event.libraryId)
    }

    private val onSearchStateFlow = MutableStateFlow("")

    private val downloadsFilterEnabled: Boolean
        get() {
            val filters = ScreenArguments.filterArgs.filters
            return filters.any { it is ItemsFilter.downloadedFiles }
        }

    fun init() = initOnce {
        EventBus.getDefault().register(this)
        val filterArgs = ScreenArguments.filterArgs
        updateState {
            copy(
                isDownloadsChecked = downloadsFilterEnabled,
                selectedTags = filterArgs.selectedTags,
//                displayAll = defaults.isTagPickerDisplayAllTags(),
                showAutomatic = defaults.isTagPickerShowAutomaticTags()
            )
        }
        onSearchStateFlow
            .drop(1)
            .debounce(150)
            .map { text ->
                search(text)
            }
            .launchIn(viewModelScope)
        itemsDidChange(
            filters = filterArgs.filters,
            collectionId = filterArgs.collectionId,
            libraryId = filterArgs.libraryId
        )
    }

    private fun search(term: String) {
        if (!term.isEmpty()) {
            val filtered =
                viewState.tags
                    .flatten()
                    .filter { it.tag.name.contains(term, ignoreCase = true) }.chunked(LIST_CHUNK_SIZE)
            if (viewState.snapshot == null) {
                updateState {
                    copy(snapshot = viewState.tags)
                }
            }
            updateState {
                copy(
                    tags = filtered,
                    searchTerm = term
                )
            }
        } else {
            val snapshot = viewState.snapshot ?: return
            updateState {
                copy(
                    tags = snapshot,
                    snapshot = null,
                    searchTerm = ""
                )
            }
        }
    }

    fun onDone() {
        triggerEffect(FilterViewEffect.OnBack)
    }

    fun onDownloadsTapped() {
        updateState {
            copy(isDownloadsChecked = !viewState.isDownloadsChecked)
        }
        if (viewState.isDownloadsChecked) {
            EventBus.getDefault().post(FilterResult.enableFilter(ItemsFilter.downloadedFiles))
        } else {
            EventBus.getDefault().post(FilterResult.disableFilter(ItemsFilter.downloadedFiles))
        }
    }

    fun onSearch(text: String) {
        updateState {
            copy(searchTerm = text)
        }
        onSearchStateFlow.tryEmit(text)
    }

    fun dismissBottomSheet() {
        updateState {
            copy(
                showFilterOptionsPopup = false
            )
        }
    }

    fun onLongPressOptionsItemSelected(longPressOptionItem: LongPressOptionItem) {
        viewModelScope.launch {
            when (longPressOptionItem) {
                is LongPressOptionItem.DisplayAllTagsInThisLibraryChecked -> {
                    setDisplayAll(!viewState.displayAll)
                }
                is LongPressOptionItem.DisplayAllTagsInThisLibraryUnchecked -> {
                    setDisplayAll(!viewState.displayAll)
                }

                else -> {}
            }
        }
    }

    fun onMoreSearchOptionsClicked() {
        updateState {
            copy(
                showFilterOptionsPopup = true,
            )
        }
    }

    fun deselectAll() {
        updateState {
            copy(
                selectedTags = emptySet(),
                showFilterOptionsPopup = false
            )
        }
        EventBus.getDefault().post(FilterResult.tagSelectionDidChange(viewState.selectedTags))
    }

    fun onTagTapped(tag: FilterTag) {
        if (!tag.isActive) {
            return
        }
        val name = tag.tag.name
        if (viewState.selectedTags.contains(name)) {
            deselect(name)
        } else {
            select(name)
        }
        EventBus.getDefault().post(FilterResult.tagSelectionDidChange(viewState.selectedTags))
    }

    private fun select(name: String) {
        updateState {
            copy(selectedTags = viewState.selectedTags + name)
        }
    }

    private fun deselect(name: String) {
        updateState {
            copy(selectedTags = viewState.selectedTags - name)
        }
    }

    private fun itemsDidChange(
        filters: List<ItemsFilter>,
        collectionId: CollectionIdentifier,
        libraryId: LibraryIdentifier
    ) {
        viewModelScope.launch {
            withContext(dispatchers.io) {
                load(filters = filters, collectionId = collectionId, libraryId = libraryId)
            }
        }
    }

    private fun load(
        filters: List<ItemsFilter>,
        collectionId: CollectionIdentifier,
        libraryId: LibraryIdentifier
    ) {
        try {
            val selected = mutableSetOf<String>()
            var snapshot: List<FilterTag>? = null
            var sorted = mutableListOf<FilterTag>()
            var chunkedSorted = emptyList<List<FilterTag>>()
            val comparator: (FilterTag, FilterTag) -> Int = comparator@{ first, second ->
                if (!first.tag.color.isEmpty() && second.tag.color.isEmpty()) {
                    return@comparator 1
                }
                if (first.tag.color.isEmpty() && !second.tag.color.isEmpty()) {
                    return@comparator -1
                }
                if(first.tag.name[0].isLetter() && !second.tag.name[0].isLetter()) {
                    return@comparator 1
                }
                if(!first.tag.name[0].isLetter() && second.tag.name[0].isLetter()) {
                    return@comparator -1
                }

                if(first.tag.name[0].isDigit() && !second.tag.name[0].isDigit()) {
                    return@comparator 1
                }
                if(!first.tag.name[0].isDigit() && second.tag.name[0].isDigit()) {
                    return@comparator -1
                }

                first.tag.name.compareTo(
                    second.tag.name,
                    ignoreCase = true
                )
            }
            dbWrapperMain.realmDbStorage.perform { coordinator ->
                val filtered = coordinator.perform(
                    request = ReadFilteredTagsDbRequest(
                        collectionId = collectionId,
                        libraryId = libraryId,
                        showAutomatic = viewState.showAutomatic,
                        filters = filters
                    )
                )
                val colored =
                    coordinator.perform(request = ReadColoredTagsDbRequest(libraryId = libraryId)) //TODO!!!!

                for (tag in filtered) {
                    if (!viewState.selectedTags.contains(tag.name)) {
                        continue
                    }
                    selected.add(tag.name)
                }

//                for (rTag in colored) {
//                    val tag = Tag(tag = rTag)
//                    val isActive = filtered.contains(tag)
//                    val filterTag = FilterTag(tag = tag, isActive = isActive)
//                    val index = sorted.index(filterTag, sortedBy = comparator)
//                    sorted.add(element = filterTag, index = index)
//                }
                sorted.addAll(colored
                    .map {
                        val tag = Tag(tag = it)
                        val isActive = filtered.contains(tag)
                        val filterTag = FilterTag(tag = tag, isActive = isActive)
                        filterTag
                    }
                    .sortedWith(comparator)
                )

                if (!viewState.displayAll) {
//                    for (tag in filtered) {
//                        if (!tag.color.isEmpty()) {
//                            continue
//                        }
//                        val filterTag = FilterTag(tag = tag, isActive = true)
//                        val index = sorted.index(filterTag, sortedBy = comparator)
//                        sorted.add(element = filterTag, index = index)
//                    }
                    sorted.addAll(filtered.filter { it.color.isEmpty() }
                        .map { FilterTag(tag = it, isActive = true) }
                        .sortedWith(comparator))
                } else {
                    val tags = coordinator.perform(
                        request = ReadFilteredTagsDbRequest(
                            collectionId = CollectionIdentifier.custom(
                                CollectionIdentifier.CustomType.all
                            ),
                            libraryId = libraryId,
                            showAutomatic = viewState.showAutomatic,
                            filters = emptyList()
                        )
                    )

                    sorted.addAll(tags.filter { it.color.isEmpty() }
                        .map {
                            val isActive = filtered.contains(it)
                            val filterTag = FilterTag(tag = it, isActive = isActive)
                            filterTag
                        }
                        .sortedWith(comparator))

//                    for (tag in tags) {
//                        if (!tag.color.isEmpty()) {
//                            continue
//                        }
//                        val isActive = filtered.contains(tag)
//                        val filterTag = FilterTag(tag = tag, isActive = isActive)
//                        val index = sorted.index(filterTag, sortedBy = comparator)
//                        sorted.add(element = filterTag, index = index)
//                    }
                }

                coordinator.invalidate()
                if (!viewState.searchTerm.isEmpty()) {
                    // Perform search filter if needed
                    snapshot = sorted
                    sorted = sorted.filter {
                        it.tag.name.contains(
                            viewState.searchTerm,
                            ignoreCase = true
                        )
                    }.toMutableList()
                }
                chunkedSorted = sorted.chunked(LIST_CHUNK_SIZE)
            }
            viewModelScope.launch {
                updateState {
                    copy(
                        tags = chunkedSorted,
                        snapshot = snapshot?.chunked(LIST_CHUNK_SIZE),
                        selectedTags = selected
                    )
                }
            }
        } catch (error: Exception) {
            Timber.e(error, "FilterViewModel: can't load tag")
        }
    }

    override fun onCleared() {
        EventBus.getDefault().unregister(this)
    }

    fun setShowAutomatic(showAutomatic: Boolean) {
        defaults.setTagPickerShowAutomaticTags(showAutomatic)
        updateState {
            copy(
                showAutomatic = showAutomatic,
                showFilterOptionsPopup = false
            )
        }
        itemsDidChange(
            filters = ScreenArguments.filterArgs.filters,
            collectionId = ScreenArguments.filterArgs.collectionId,
            libraryId = ScreenArguments.filterArgs.libraryId
        )
    }

    private fun setDisplayAll(displayAll: Boolean) {
        defaults.setTagPickerDisplayAllTags(displayAll)
        updateState {
            copy(displayAll = displayAll)
        }
        itemsDidChange(
            filters = ScreenArguments.filterArgs.filters,
            collectionId = ScreenArguments.filterArgs.collectionId,
            libraryId = ScreenArguments.filterArgs.libraryId
        )

    }

    fun loadAutomaticCount() {
        updateState {
            copy(showFilterOptionsPopup = false)
        }
        val request = ReadAutomaticTagsDbRequest(libraryId = ScreenArguments.filterArgs.libraryId)
        val count = dbWrapperMain.realmDbStorage.perform(request = request).size
        confirmDeletion(count)
    }

    private fun confirmDeletion(count: Int) {
        updateState {
            copy(dialog = FilterDialog.confirmDeletion(count))
        }

    }

    fun deleteAutomaticTags() {
        viewModelScope.launch {
            perform(
                dbWrapper = dbWrapperMain,
                request = DeleteAutomaticTagsDbRequest(libraryId = ScreenArguments.filterArgs.libraryId)
            )
        }
    }

    fun onDismissDialog() {
        updateState {
            copy(
                dialog = null,
            )
        }
    }

}

internal data class FilterViewState(
    val isDownloadsChecked: Boolean = false,
    val searchTerm: String = "",
    val showFilterOptionsPopup: Boolean = false,
    val showAutomatic: Boolean = false,
    val displayAll: Boolean = false,
    val tags: List<List<FilterTag>> = emptyList(),
    val snapshot: List<List<FilterTag>>? = null,
    val selectedTags: Set<String> = emptySet(),
    val dialog: FilterDialog? = null,
) : ViewState

internal sealed class FilterViewEffect : ViewEffect {
    object OnBack : FilterViewEffect()
}
package org.zotero.android.screens.reader.filter

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.database.objects.RCustomLibraryType
import org.zotero.android.pdf.data.PdfReaderCurrentThemeEventStream
import org.zotero.android.screens.reader.data.ReaderAnnotationsFilter
import org.zotero.android.screens.reader.filter.data.ReaderFilterArgs
import org.zotero.android.screens.reader.filter.data.ReaderFilterResult
import org.zotero.android.screens.tagpicker.data.TagPickerArgs
import org.zotero.android.screens.tagpicker.data.TagPickerResult
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.Tag
import javax.inject.Inject

@HiltViewModel
internal class ReaderFilterViewModel @Inject constructor(
    private val pdfReaderCurrentThemeEventStream: PdfReaderCurrentThemeEventStream,
) : BaseViewModel2<ReaderFilterViewState, ReaderFilterViewEffect>(ReaderFilterViewState()) {

    private var pdfReaderThemeCancellable: Job? = null

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(tagPickerResult: TagPickerResult) {
        if (tagPickerResult.callPoint == TagPickerResult.CallPoint.ReaderFilter) {
            viewModelScope.launch {
                setTags(tagPickerResult.tags)
            }
        }
    }

    private fun setTags(tags: List<Tag>) {
        updateState {
            copy(tags = tags.map { it.name }.toSet())
        }
        updateFilter()

    }

    fun init(args: ReaderFilterArgs) = initOnce {
        updateState {
            copy(isDark = pdfReaderCurrentThemeEventStream.currentValue()!!.isDark)
        }
        startObservingTheme()

        EventBus.getDefault().register(this)

        updateState {
            copy(
                colors = args.filter?.colors ?: emptySet(),
                tags = args.filter?.tags ?: emptySet(),
                availableColors = args.availableColors,
                availableTags = args.availableTags,
            )
        }
    }

    fun onClose() {
        triggerEffect(ReaderFilterViewEffect.OnBack)
        updateFilter()
    }

    private fun updateFilter() {
        if (viewState.colors.isEmpty() && viewState.tags.isEmpty()) {
            EventBus.getDefault().post(ReaderFilterResult(null))
        } else {
            val filter = ReaderAnnotationsFilter(colors = viewState.colors, tags = viewState.tags)
            EventBus.getDefault().post(ReaderFilterResult(filter))
        }
    }

    fun onClear() {
        updateState {
            copy(tags = emptySet(), colors = emptySet())
        }
        updateFilter()
    }

    fun toggleColor(color: String) {
        if (!viewState.availableColors.contains(color)) { return }
        if (viewState.colors.contains(color)) {
            updateState {
                copy(colors = viewState.colors - color)
            }
        } else {
            updateState {
                copy(colors = viewState.colors + color)
            }
        }
        updateFilter()
    }

    fun onTagsClicked() {
        ScreenArguments.tagPickerArgs = TagPickerArgs(
            libraryId = LibraryIdentifier.custom(RCustomLibraryType.myLibrary),
            selectedTags = viewState.tags,
            tags = viewState.availableTags,
            callPoint = TagPickerResult.CallPoint.ReaderFilter,
        )

        triggerEffect(ReaderFilterViewEffect.NavigateToTagPickerScreen)
    }

    override fun onCleared() {
        super.onCleared()
        EventBus.getDefault().unregister(this)
    }

    private fun startObservingTheme() {
        this.pdfReaderThemeCancellable = pdfReaderCurrentThemeEventStream.flow()
            .onEach { data ->
                updateState {
                    copy(isDark = data!!.isDark)
                }
            }
            .launchIn(viewModelScope)
    }
}

internal data class ReaderFilterViewState(
    val availableColors: List<String> = emptyList(),
    val availableTags: List<Tag> = emptyList(),
    var colors: Set<String> = emptySet(),
    var tags: Set<String> = emptySet(),
    val isDark: Boolean = false,
) : ViewState {
    fun isClearVisible(): Boolean {
        return !colors.isEmpty() || !tags.isEmpty()
    }

    fun formattedTags(): String {
        return tags.joinToString(separator = ", ")
    }
}

internal sealed class ReaderFilterViewEffect : ViewEffect {
    object OnBack: ReaderFilterViewEffect()
    object NavigateToTagPickerScreen: ReaderFilterViewEffect()
}
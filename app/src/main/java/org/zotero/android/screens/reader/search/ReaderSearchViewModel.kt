package org.zotero.android.screens.reader.search

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.greenrobot.eventbus.EventBus
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.pdf.data.PdfReaderCurrentThemeEventStream
import org.zotero.android.pdf.data.PdfReaderThemeDecider
import org.zotero.android.screens.reader.search.data.ReaderSearchItem
import org.zotero.android.screens.reader.search.data.ReaderSearchResultSelected
import org.zotero.android.screens.reader.search.data.ReaderSearchResultsEventStream
import org.zotero.android.screens.reader.search.data.ReaderSearchTermData
import org.zotero.android.screens.reader.search.data.ReaderSearchTermEventStream
import javax.inject.Inject

@HiltViewModel
internal class ReaderSearchViewModel @Inject constructor(
    private val pdfReaderCurrentThemeEventStream: PdfReaderCurrentThemeEventStream,
    private val pdfReaderThemeDecider: PdfReaderThemeDecider,
    private val readerSearchResultsEventStream: ReaderSearchResultsEventStream,
    private val readerSearchTermEventStream: ReaderSearchTermEventStream,
) : BaseViewModel2<ReaderSearchViewState, ReaderSearchViewEffect>(ReaderSearchViewState()) {

    private val onSearchStateFlow = MutableStateFlow("")

    private var pdfReaderThemeCancellable: Job? = null
    private var readerSearchResultsCancellable: Job? = null

    private fun startObservingTheme() {
        this.pdfReaderThemeCancellable = pdfReaderCurrentThemeEventStream.flow()
            .onEach { data ->
                updateState {
                    copy(isDark = data!!.isDark)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun startObservingSearchResults() {
        this.readerSearchResultsCancellable = readerSearchResultsEventStream.flow()
            .onEach { data ->
                processDocumentSearchResults(data?.data)
            }
            .launchIn(viewModelScope)
    }


    fun init() = initOnce {
        updateState {
            copy(isDark = pdfReaderCurrentThemeEventStream.currentValue()!!.isDark)
        }
        startObservingTheme()
        startObservingSearchResults()


        onSearchStateFlow
            .drop(1)
            .debounce(150)
            .map { text ->
                search(text)
            }
            .launchIn(viewModelScope)

    }

    private fun search(term: String) {
        readerSearchTermEventStream.emit(ReaderSearchTermData(term))
    }

    fun onSearch(text: String) {
        updateState {
            copy(searchTerm = text)
        }
        onSearchStateFlow.tryEmit(text)
    }

    fun onItemTapped(searchItem: ReaderSearchItem) {
        triggerEffect(ReaderSearchViewEffect.OnBack)
        EventBus.getDefault().post(ReaderSearchResultSelected(searchItem.index))

    }

    fun setOsTheme(isDark: Boolean) {
        pdfReaderThemeDecider.setCurrentOsTheme(isOsThemeDark = isDark)
    }

    private fun processDocumentSearchResults(data: JsonObject?) {
        var p = data?.get("params")
        if (p?.isJsonNull == true) {
            p = null
        }
        val params = p?.asJsonObject
        val snippets = params?.get("snippets")?.asJsonArray?.map { it.asString }
        val term = viewState.searchTerm
        if (params == null || snippets == null || term.isEmpty()) {
            updateState {
                copy(searchResults = emptyList())
            }
            return
        }
        val result = snippets.mapIndexed { index, snippet ->
            val annotatedString = buildAnnotatedString {
                val highlightStart = snippet.indexOf(term)
                val highlightEnd = highlightStart + term.length
                val previewText = snippet
                append(previewText)
                addStyle(
                    style = SpanStyle(background = Color.Yellow),
                    start = highlightStart,
                    end = highlightEnd
                )
            }
            ReaderSearchItem(
                index = index,
                annotatedString = annotatedString
            )
        }
        updateState {
            copy(searchResults = result)
        }
    }

}

internal data class ReaderSearchViewState(
    val searchTerm: String = "",
    val searchResults: List<ReaderSearchItem> = emptyList(),
    val isDark: Boolean = false,
) : ViewState

internal sealed class ReaderSearchViewEffect : ViewEffect {
    object OnBack : ReaderSearchViewEffect()
}
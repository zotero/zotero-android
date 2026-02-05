package org.zotero.android.screens.htmlepub.reader.search

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
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.pdf.data.PdfReaderCurrentThemeEventStream
import org.zotero.android.pdf.data.PdfReaderThemeDecider
import org.zotero.android.screens.htmlepub.reader.search.data.HtmlEpubReaderSearchItem
import org.zotero.android.screens.htmlepub.reader.search.data.HtmlEpubReaderSearchResultSelected
import org.zotero.android.screens.htmlepub.reader.search.data.HtmlEpubReaderSearchResultsEventStream
import org.zotero.android.screens.htmlepub.reader.search.data.HtmlEpubReaderSearchTermData
import org.zotero.android.screens.htmlepub.reader.search.data.HtmlEpubReaderSearchTermEventStream
import javax.inject.Inject

@HiltViewModel
internal class HtmlEpubReaderSearchViewModel @Inject constructor(
    private val dispatchers: Dispatchers,
    private val pdfReaderCurrentThemeEventStream: PdfReaderCurrentThemeEventStream,
    private val pdfReaderThemeDecider: PdfReaderThemeDecider,
    private val htmlEpubReaderSearchResultsEventStream: HtmlEpubReaderSearchResultsEventStream,
    private val htmlEpubReaderSearchTermEventStream: HtmlEpubReaderSearchTermEventStream,
) : BaseViewModel2<HtmlEpubReaderSearchViewState, HtmlEpubReaderSearchViewEffect>(HtmlEpubReaderSearchViewState()) {

    private val onSearchStateFlow = MutableStateFlow("")

    private var pdfReaderThemeCancellable: Job? = null
    private var htmlEpubReaderSearchResultsCancellable: Job? = null

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
        this.htmlEpubReaderSearchResultsCancellable = htmlEpubReaderSearchResultsEventStream.flow()
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


//        val args = ScreenArguments.htmlEpubReaderSearchArgs

        onSearchStateFlow
            .drop(1)
            .debounce(150)
            .map { text ->
                search(text)
            }
            .launchIn(viewModelScope)

    }

    private fun search(term: String) {
        htmlEpubReaderSearchTermEventStream.emit(HtmlEpubReaderSearchTermData(term))
    }

    fun onSearch(text: String) {
        updateState {
            copy(searchTerm = text)
        }
        onSearchStateFlow.tryEmit(text)
    }

    fun onItemTapped(searchItem: HtmlEpubReaderSearchItem) {

        triggerEffect(HtmlEpubReaderSearchViewEffect.OnBack)
        EventBus.getDefault().post(HtmlEpubReaderSearchResultSelected())

    }

    fun setOsTheme(isDark: Boolean) {
        pdfReaderThemeDecider.setCurrentOsTheme(isOsThemeDark = isDark)
    }

    private fun processDocumentSearchResults(data: JsonObject?) {
        val params = data?.get("params")?.asJsonObject
        val snippets = params?.get("snippets")?.asJsonArray?.map { it.asString }
        val term = viewState.searchTerm
        if (params == null || snippets == null || term.isEmpty()) {
            updateState {
                copy(searchResults = emptyList())
            }
            return
        }
        val result = snippets.map { snippet ->
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
            HtmlEpubReaderSearchItem(
                annotatedString = annotatedString
            )
        }
        updateState {
            copy(searchResults = result)
        }
    }

}

internal data class HtmlEpubReaderSearchViewState(
    val searchTerm: String = "",
    val searchResults: List<HtmlEpubReaderSearchItem> = emptyList(),
    val isDark: Boolean = false,
) : ViewState

internal sealed class HtmlEpubReaderSearchViewEffect : ViewEffect {
    object OnBack : HtmlEpubReaderSearchViewEffect()
}
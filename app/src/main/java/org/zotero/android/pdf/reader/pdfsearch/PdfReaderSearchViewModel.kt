package org.zotero.android.pdf.reader.pdfsearch

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.lifecycle.viewModelScope
import com.pspdfkit.document.search.CompareOptions
import com.pspdfkit.document.search.SearchOptions
import com.pspdfkit.document.search.SearchResult
import com.pspdfkit.document.search.TextSearch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.pdf.data.PdfReaderCurrentThemeEventStream
import org.zotero.android.pdf.data.PdfReaderThemeDecider
import org.zotero.android.pdf.reader.pdfsearch.data.OnPdfReaderSearch
import org.zotero.android.pdf.reader.pdfsearch.data.PdfReaderSearchItem
import org.zotero.android.pdf.reader.pdfsearch.data.PdfReaderSearchResultSelected
import javax.inject.Inject

@HiltViewModel
internal class PdfReaderSearchViewModel @Inject constructor(
    private val dispatchers: Dispatchers,
    private val pdfReaderCurrentThemeEventStream: PdfReaderCurrentThemeEventStream,
    private val pdfReaderThemeDecider: PdfReaderThemeDecider,
) : BaseViewModel2<PdfReaderSearchViewState, PdfReaderSearchViewEffect>(PdfReaderSearchViewState()) {

    private val onSearchStateFlow = MutableStateFlow("")

    private var pdfReaderThemeCancellable: Job? = null
    private lateinit var searchResults: List<SearchResult>

    private lateinit var textSearch: TextSearch
    private val searchOptions: SearchOptions = SearchOptions.Builder()
        .compareOptions(CompareOptions.CASE_INSENSITIVE, CompareOptions.DIACRITIC_INSENSITIVE)
        .build()

    private fun startObservingTheme() {
        this.pdfReaderThemeCancellable = pdfReaderCurrentThemeEventStream.flow()
            .onEach { data ->
                updateState {
                    copy(isDark = data!!.isDark)
                }
            }
            .launchIn(viewModelScope)
    }


    fun init() = initOnce {
        updateState {
            copy(isDark = pdfReaderCurrentThemeEventStream.currentValue()!!.isDark)
        }
        startObservingTheme()


        val args = ScreenArguments.pdfReaderSearchArgs
        textSearch = TextSearch(args.pdfDocument, args.configuration)

        onSearchStateFlow
            .drop(1)
            .debounce(150)
            .map { text ->
                search(text)
            }
            .launchIn(viewModelScope)

    }

    private fun search(term: String) {
        if (!term.isEmpty()) {
            viewModelScope.launch {
                withContext(dispatchers.io) {
                    searchResults = textSearch.performSearch(term, searchOptions)
                    val rows = searchResults.mapNotNull {
                        val snippet = it.snippet ?: return@mapNotNull null

                        val annotatedString = buildAnnotatedString {
                            val highlightStart = snippet.rangeInSnippet.startPosition
                            val highlightEnd = snippet.rangeInSnippet.endPosition
                            val previewText = snippet.text
                            append(previewText)
                            addStyle(
                                style = SpanStyle(background = Color.Yellow),
                                start = highlightStart,
                                end = highlightEnd
                            )
                        }
                        PdfReaderSearchItem(
                            pageNumber = it.pageIndex,
                            annotatedString = annotatedString
                        )
                    }
                    EventBus.getDefault().post(OnPdfReaderSearch(searchResults))
                    viewModelScope.launch {
                        updateState {
                            copy(searchResults = rows)
                        }
                    }

                }
            }
        } else {
            EventBus.getDefault().post(OnPdfReaderSearch(emptyList()))
            updateState {
                copy(
                    searchResults = emptyList()
                )
            }
        }
    }

    fun onSearch(text: String) {
        updateState {
            copy(searchTerm = text)
        }
        onSearchStateFlow.tryEmit(text)
    }

    fun onItemTapped(searchItem: PdfReaderSearchItem) {
        val searchResult = searchResults[viewState.searchResults.indexOf(searchItem)]

        triggerEffect(PdfReaderSearchViewEffect.OnBack)
        EventBus.getDefault().post(PdfReaderSearchResultSelected(searchResult))

    }

    fun setOsTheme(isDark: Boolean) {
        pdfReaderThemeDecider.setCurrentOsTheme(isOsThemeDark = isDark)
    }

}

internal data class PdfReaderSearchViewState(
    val searchTerm: String = "",
    val searchResults: List<PdfReaderSearchItem> = emptyList(),
    val isDark: Boolean = false,
) : ViewState

internal sealed class PdfReaderSearchViewEffect : ViewEffect {
    object OnBack : PdfReaderSearchViewEffect()
}
package org.zotero.android.screens.htmlepub.reader.search

import androidx.lifecycle.viewModelScope
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
import org.zotero.android.screens.htmlepub.reader.search.data.HtmlEpubReaderSearchItem
import org.zotero.android.screens.htmlepub.reader.search.data.HtmlEpubReaderSearchResultSelected
import org.zotero.android.screens.htmlepub.reader.search.data.OnHtmlEpubReaderSearch
import javax.inject.Inject

@HiltViewModel
internal class HtmlEpubReaderSearchViewModel @Inject constructor(
    private val dispatchers: Dispatchers,
    private val pdfReaderCurrentThemeEventStream: PdfReaderCurrentThemeEventStream,
    private val pdfReaderThemeDecider: PdfReaderThemeDecider,
) : BaseViewModel2<HtmlEpubReaderSearchViewState, HtmlEpubReaderSearchViewEffect>(HtmlEpubReaderSearchViewState()) {

    private val onSearchStateFlow = MutableStateFlow("")

    private var pdfReaderThemeCancellable: Job? = null

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


        val args = ScreenArguments.htmlEpubReaderSearchArgs

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
                    //TODO
                }
            }
        } else {
            EventBus.getDefault().post(OnHtmlEpubReaderSearch())
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

    fun onItemTapped(searchItem: HtmlEpubReaderSearchItem) {

        triggerEffect(HtmlEpubReaderSearchViewEffect.OnBack)
        EventBus.getDefault().post(HtmlEpubReaderSearchResultSelected())

    }

    fun setOsTheme(isDark: Boolean) {
        pdfReaderThemeDecider.setCurrentOsTheme(isOsThemeDark = isDark)
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
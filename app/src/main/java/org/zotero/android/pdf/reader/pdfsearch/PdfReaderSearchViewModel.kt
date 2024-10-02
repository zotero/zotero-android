package org.zotero.android.pdf.reader.pdfsearch

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.database.DbWrapperMain
import org.zotero.android.pdf.data.PdfReaderCurrentThemeEventStream
import org.zotero.android.pdf.data.PdfReaderThemeDecider
import org.zotero.android.pdf.reader.pdfsearch.data.PdfReaderSearchItem
import javax.inject.Inject

@HiltViewModel
internal class PdfReaderSearchViewModel @Inject constructor(
    private val dbWrapperMain: DbWrapperMain,
    private val defaults: Defaults,
    private val dispatchers: Dispatchers,
    private val pdfReaderCurrentThemeEventStream: PdfReaderCurrentThemeEventStream,
    private val pdfReaderThemeDecider: PdfReaderThemeDecider,
) : BaseViewModel2<PdfReaderSearchViewState, PdfReaderSearchViewEffect>(PdfReaderSearchViewState()) {

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


        val args = ScreenArguments.pdfReaderSearchArgs

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

        } else {
            updateState {
                copy(
                    searchResults = emptyList()
                )
            }
        }
    }

    fun onDone() {
        triggerEffect(PdfReaderSearchViewEffect.OnBack)
    }


    fun onSearch(text: String) {
        updateState {
            copy(searchTerm = text)
        }
        onSearchStateFlow.tryEmit(text)
    }

    fun onItemTapped() {


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
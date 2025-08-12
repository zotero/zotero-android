package org.zotero.android.pdf.reader.plainreader

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.pspdfkit.configuration.PdfConfiguration
import com.pspdfkit.configuration.theming.ThemeMode
import com.pspdfkit.document.PdfDocumentLoader
import com.pspdfkit.ui.PdfReaderView
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.architecture.navigation.NavigationParamsMarshaller
import org.zotero.android.architecture.require
import org.zotero.android.pdf.ARG_PDF_PLAIN_READER_SCREEN
import org.zotero.android.pdf.data.PdfReaderCurrentThemeEventStream
import org.zotero.android.pdf.data.PdfReaderThemeDecider
import org.zotero.android.pdf.reader.plainreader.data.PdfPlainReaderArgs
import javax.inject.Inject

@HiltViewModel
class PdfPlainReaderViewModel @Inject constructor(
    private val pdfReaderCurrentThemeEventStream: PdfReaderCurrentThemeEventStream,
    private val pdfReaderThemeDecider: PdfReaderThemeDecider,
    private val defaults: Defaults,
    private val context: Context,
    private val navigationParamsMarshaller: NavigationParamsMarshaller,
    stateHandle: SavedStateHandle,
) : BaseViewModel2<PdfPlainReaderViewState, PdfPlainReaderViewEffect>(PdfPlainReaderViewState()) {

    private val screenArgs: PdfPlainReaderArgs by lazy {
        val argsEncoded = stateHandle.get<String>(ARG_PDF_PLAIN_READER_SCREEN).require()
        navigationParamsMarshaller.decodeObjectFromBase64(argsEncoded)
    }

    fun init(
        pdfReaderView: PdfReaderView
    ) {
        startObservingTheme()

        val pdfSettings = defaults.getPDFSettings()
        pdfReaderThemeDecider.setPdfPageAppearanceMode(pdfSettings.appearanceMode)

        val isCalculatedThemeDark = pdfReaderCurrentThemeEventStream.currentValue()!!.isDark
        val themeMode = when (isCalculatedThemeDark) {
            true -> ThemeMode.NIGHT
            false -> ThemeMode.DEFAULT
        }

        val configuration = PdfConfiguration.Builder()
            .invertColors(isCalculatedThemeDark)
            .themeMode(themeMode)
            .build()

        val document = PdfDocumentLoader.openDocument(this.context, screenArgs.uri)
        pdfReaderView.setDocument(document, configuration)
        pdfReaderView.show()

        updateState {
            copy(title = document.title ?: "")
        }
    }

    private var pdfReaderThemeCancellable: Job? = null

    private fun startObservingTheme() {
        this.pdfReaderThemeCancellable = pdfReaderCurrentThemeEventStream.flow()
            .onEach { data ->
                val isDark = data!!.isDark
                updateState {
                    copy(isDark = isDark)
                }
                triggerEffect(PdfPlainReaderViewEffect.ScreenRefresh)
            }
            .launchIn(viewModelScope)
    }

    fun setOsTheme(isDark: Boolean) {
        pdfReaderThemeDecider.setCurrentOsTheme(isOsThemeDark = isDark)
    }

}

data class PdfPlainReaderViewState(
    val isDark: Boolean = false,
    val title: String = "",
) : ViewState

sealed class PdfPlainReaderViewEffect : ViewEffect {
    object NavigateBack : PdfPlainReaderViewEffect()
    object ScreenRefresh : PdfPlainReaderViewEffect()
}

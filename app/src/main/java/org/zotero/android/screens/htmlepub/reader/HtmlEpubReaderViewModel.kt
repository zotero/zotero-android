package org.zotero.android.screens.htmlepub.reader

import android.content.Context
import android.net.Uri
import androidx.core.net.toFile
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.architecture.navigation.NavigationParamsMarshaller
import org.zotero.android.architecture.require
import org.zotero.android.files.FileStore
import org.zotero.android.pdf.data.PdfReaderCurrentThemeEventStream
import org.zotero.android.pdf.data.PdfReaderThemeDecider
import org.zotero.android.screens.htmlepub.ARG_HTML_EPUB_READER_SCREEN
import org.zotero.android.screens.htmlepub.reader.data.HtmlEpubReaderArgs
import org.zotero.android.sync.Library
import org.zotero.android.sync.LibraryIdentifier
import java.io.File
import java.util.Timer
import javax.inject.Inject
import kotlin.concurrent.timerTask

@HiltViewModel
class HtmlEpubReaderViewModel @Inject constructor(
    private val defaults: Defaults,
    private val pdfReaderCurrentThemeEventStream: PdfReaderCurrentThemeEventStream,
    private val pdfReaderThemeDecider: PdfReaderThemeDecider,
    private val context: Context,
    private val navigationParamsMarshaller: NavigationParamsMarshaller,
    private val dispatcher: CoroutineDispatcher,
    private val fileStore: FileStore,
    stateHandle: SavedStateHandle,
) : BaseViewModel2<HtmlEpubReaderViewState, HtmlEpubReaderViewEffect>(HtmlEpubReaderViewState())  {

    private lateinit var originalFile: File
    private lateinit var readerDirectory: File
    private lateinit var documentFile: File
    private lateinit var readerFile: File

    private var isTablet: Boolean = false

    private var disableForceScreenOnTimer: Timer? = null

    val screenArgs: HtmlEpubReaderArgs by lazy {
        val argsEncoded = stateHandle.get<String>(ARG_HTML_EPUB_READER_SCREEN).require()
        navigationParamsMarshaller.decodeObjectFromBase64(argsEncoded)
    }

    private var pdfReaderThemeCancellable: Job? = null

    private fun startObservingTheme() {
        this.pdfReaderThemeCancellable = pdfReaderCurrentThemeEventStream.flow()
            .onEach { data ->
                val isDark = data!!.isDark
                updateState {
                    copy(isDark = isDark)
                }
                triggerEffect(HtmlEpubReaderViewEffect.ScreenRefresh)
            }
            .launchIn(viewModelScope)
    }

    fun init(
        uri: Uri,
        isTablet: Boolean,
    ) {
        initFileUris(uri)
        restartDisableForceScreenOnTimer()
        this.isTablet = isTablet

        EventBus.getDefault().register(this)

        initState()
        startObservingTheme()

        initialiseReader()

    }

    private fun initialiseReader() {
        val readerUrl = fileStore.htmlEpubReaderDirectory()
        readerUrl.copyRecursively(target = readerDirectory, overwrite = true)
        originalFile.copyRecursively(target = documentFile, overwrite = true)
    }

    private fun initState() {
        val params = this.screenArgs
        updateState {
            copy(
                key = params.key,
                parentKey = params.parentKey,
                library = params.library,
            )
        }
    }


    private fun initFileUris(uri: Uri) {
        this.originalFile = uri.toFile()
        this.readerDirectory = fileStore.runningHtmlEpubReaderDirectory()
        this.documentFile = fileStore.runningHtmlEpubReaderUserFileSubDirectory()
        this.readerFile = File(readerDirectory, "view.html")
    }

    fun restartDisableForceScreenOnTimer() {
        viewModelScope.launch {
            triggerEffect(HtmlEpubReaderViewEffect.EnableForceScreenOn)
        }
        disableForceScreenOnTimer?.cancel()
        disableForceScreenOnTimer = Timer()
        disableForceScreenOnTimer?.schedule(timerTask {
            viewModelScope.launch {
                triggerEffect(HtmlEpubReaderViewEffect.DisableForceScreenOn)
            }
        }, 25 * 60 * 1000L)
    }

    fun setOsTheme(isDark: Boolean) {
        pdfReaderThemeDecider.setCurrentOsTheme(isOsThemeDark = isDark)
    }

    fun onStop() {
        disableForceScreenOnTimer?.cancel()
    }

}

data class HtmlEpubReaderViewState(
    val key: String = "",
    val parentKey: String? = null,
    val library: Library = Library(
        identifier = LibraryIdentifier.group(0),
        name = "",
        metadataEditable = false,
        filesEditable = false
    ),
    val isDark: Boolean = false,
) : ViewState {
}

sealed class HtmlEpubReaderViewEffect : ViewEffect {
    object NavigateBack : HtmlEpubReaderViewEffect()
    object DisableForceScreenOn : HtmlEpubReaderViewEffect()
    object EnableForceScreenOn : HtmlEpubReaderViewEffect()
    object ScreenRefresh: HtmlEpubReaderViewEffect()
}
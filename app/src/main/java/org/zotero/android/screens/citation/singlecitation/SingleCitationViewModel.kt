package org.zotero.android.screens.citation.singlecitation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.citation.CitationController
import org.zotero.android.citation.CitationController.Format
import org.zotero.android.citation.CitationControllerInterface
import org.zotero.android.citation.CitationControllerPreviewHeightUpdateEventStream
import org.zotero.android.citation.CitationControllerPreviewUpdateEventStream
import org.zotero.android.sync.LibraryIdentifier
import javax.inject.Inject


val locatorsList = listOf(
    "page",
    "book",
    "chapter",
    "column",
    "figure",
    "folio",
    "issue",
    "line",
    "note",
    "opus",
    "paragraph",
    "part",
    "section",
    "sub verbo",
    "verse",
    "volume"
)

@HiltViewModel
internal class SingleCitationViewModel @Inject constructor(
    private val citationController: CitationController,
    private val defaults: Defaults,
    private val citationControllerPreviewHeightUpdateEventStream: CitationControllerPreviewHeightUpdateEventStream,
    private val citationControllerPreviewUpdateEventStream: CitationControllerPreviewUpdateEventStream,
    private val context: Context,
) : BaseViewModel2<SingleCitationViewState, SingleCitationViewEffect>(SingleCitationViewState()),
    CitationControllerInterface {

    private var libraryId: LibraryIdentifier = LibraryIdentifier.group(0)
    private var itemIds: Set<String> = emptySet()
    private var styleId: String = ""
    private var localeId: String = ""
    private var exportAsHtml: Boolean = false

    private val onLocatorValueChangedFlow = MutableStateFlow("")

    fun init() = initOnce {
        setupObservers()
        initViewState()
        preload()
    }

    private fun setupObservers() {
        citationControllerPreviewUpdateEventStream.flow()
            .onEach { p  ->
                val (text, showInWebView) = p
                if (showInWebView) {
                    updateState {
                        copy(preview = text)
                    }
                } else {
                    copyHtmlToClipboard(viewState.preview, text = text)
                    triggerEffect(SingleCitationViewEffect.OnBack)
                }

            }
            .launchIn(viewModelScope)
        citationControllerPreviewHeightUpdateEventStream.flow()
            .onEach { previewHeight ->
                updateState {
                    copy(previewHeight = previewHeight)
                }
            }
            .launchIn(viewModelScope)
        onLocatorValueChangedFlow
            .drop(1)
            .debounce(150)
            .map { value ->
                loadPreview(
                    locatorLabel = viewState.locator,
                    locatorValue = value,
                    omitAuthor = viewState.omitAuthor,
                )
            }
            .launchIn(viewModelScope)
    }

    private fun copyPlainTextToClipboard(text: String) {
        val clipboard: ClipboardManager =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Zotero", text)
        clipboard.setPrimaryClip(clip)
    }

    private fun copyHtmlToClipboard(html: String, text: String) {
        val clipboard: ClipboardManager =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newHtmlText("Zotero", text, html)
        clipboard.setPrimaryClip(clip)
    }

    private fun initViewState() {
        val args = ScreenArguments.singleCitationArgs
        this.libraryId = args.libraryId
        this.itemIds = args.itemIds
        this.styleId = defaults.getQuickCopyStyleId()
        this.localeId = defaults.getQuickCopyCslLocaleId()
        this.exportAsHtml = defaults.isQuickCopyAsHtml()
    }

    fun preload() {
        citationController.init(
            citationControllerInterface = this,
            itemIds = this.itemIds,
            libraryId = this.libraryId,
            styleId = this.styleId,
            localeId = this.localeId
        )
    }

    override fun onCleared() {
        super.onCleared()
    }

    fun onCopyTapped() {
        viewModelScope.launch {
            val preview = viewState.preview.ifEmpty { return@launch }

            if (this@SingleCitationViewModel.exportAsHtml) {
                copyPlainTextToClipboard(preview)
                triggerEffect(SingleCitationViewEffect.OnBack)
                return@launch
            }
            citationController.citation(
                label = viewState.locator,
                locator = viewState.locatorValue,
                omitAuthor = viewState.omitAuthor,
                format = Format.text,
                showInWebView = false
            )
        }

    }

    fun setLocator(locator: String) {
        updateState {
            copy(locator = locator)
        }
        viewModelScope.launch {
            loadPreview(
                locatorLabel = locator,
                locatorValue = viewState.locatorValue,
                omitAuthor = viewState.omitAuthor,
            )
        }
    }

    fun onOmitAuthor(omitAuthor: Boolean) {
        updateState {
            copy(omitAuthor = omitAuthor)
        }
        viewModelScope.launch {
            loadPreview(
                locatorLabel = viewState.locator,
                locatorValue = viewState.locatorValue,
                omitAuthor = omitAuthor,
            )
        }

    }

    fun onLocatorValueChanged(locatorValue: String) {
        updateState {
            copy(locatorValue = locatorValue)
        }
        onLocatorValueChangedFlow.tryEmit(locatorValue)
    }

    override fun getLocator(): String {
        return viewState.locator
    }

    override fun getLocatorValue(): String {
        return viewState.locatorValue
    }

    override fun omitAuthor(): Boolean {
        return viewState.omitAuthor
    }

    private suspend fun loadPreview(
        locatorLabel: String,
        locatorValue: String,
        omitAuthor: Boolean,
    ) {
        citationController
            .citation(
                label = locatorLabel,
                locator = locatorValue,
                omitAuthor = omitAuthor,
                format = Format.html,
                showInWebView = true
            )
    }

}

internal data class SingleCitationViewState(
    val locator: String = locatorsList[0],
    val locatorValue: String = "",
    val loadingCopy: Boolean = false,
    var omitAuthor: Boolean = false,
    val preview: String = "",
    val previewHeight: Int = 20,
) : ViewState

internal sealed class SingleCitationViewEffect : ViewEffect {
    object OnBack : SingleCitationViewEffect()
}
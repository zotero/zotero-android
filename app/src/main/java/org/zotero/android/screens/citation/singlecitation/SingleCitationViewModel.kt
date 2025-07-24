package org.zotero.android.screens.citation.singlecitation

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.citation.CitationController
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
) : BaseViewModel2<SingleCitationViewState, SingleCitationViewEffect>(SingleCitationViewState()),
    CitationControllerInterface {

    private var libraryId: LibraryIdentifier = LibraryIdentifier.group(0)
    private var itemIds: Set<String> = emptySet()
    private var styleId: String = ""
    private var localeId: String = ""
    private var exportAsHtml: Boolean = false

    fun init() = initOnce {
        setupObservers()
        initViewState()
        preload()
    }

    private fun setupObservers() {
        citationControllerPreviewUpdateEventStream.flow()
            .onEach { preview ->
                updateState {
                    copy(preview = preview)
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
    }

    private fun initViewState() {
        val args = ScreenArguments.singleCitationArgs
        this.libraryId = args.libraryId
        this.itemIds = args.itemIds
        this.styleId = defaults.getQuickCopyStyleId()
        this.localeId = defaults.getQuickCopyCslLocaleId()
        this.exportAsHtml = defaults.isQuickCopyAsHtml()

        updateState {
            copy(
//                selected = args.itemIds.toImmutableSet(),
            )
        }
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

    }

    fun setLocator(locator: String) {
        updateState {
            copy(locator = locator)
        }
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

}

internal data class SingleCitationViewState(
//    val selected: ImmutableSet<String> = emptyImmutableSet(),
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
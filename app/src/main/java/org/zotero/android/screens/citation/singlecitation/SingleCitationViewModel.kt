package org.zotero.android.screens.citation.singlecitation

import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.citation.CitationController
import org.zotero.android.database.DbWrapperMain
import org.zotero.android.sync.LibraryIdentifier
import javax.inject.Inject

val locatorsList = listOf("page", "book", "chapter", "column", "figure", "folio", "issue", "line", "note", "opus", "paragraph", "part", "section", "sub verbo", "verse", "volume")

@HiltViewModel
internal class SingleCitationViewModel @Inject constructor(
    private val dbWrapperMain: DbWrapperMain,
    private val context: Context,
    private val citationController: CitationController,
    private val defaults: Defaults
) : BaseViewModel2<SingleCitationViewState, SingleCitationViewEffect>(SingleCitationViewState()) {

    private var libraryId: LibraryIdentifier = LibraryIdentifier.group(0)
    private var itemIds: Set<String> = emptySet()
    private var styleId: String = ""
    private var localeId: String = ""
    private var exportAsHtml: Boolean = false

    fun init() = initOnce {
        initViewState()
        preload()
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

}

internal data class SingleCitationViewState(
//    val selected: ImmutableSet<String> = emptyImmutableSet(),
    val locator: String = locatorsList[0],
    val locatorValue: String = "",
    val loadingCopy: Boolean = false,
    var omitAuthor: Boolean = false
) : ViewState

internal sealed class SingleCitationViewEffect : ViewEffect {
    object OnBack : SingleCitationViewEffect()
}
package org.zotero.android.screens.citation.singlecitation

import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableSet
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.architecture.emptyImmutableSet
import org.zotero.android.database.DbWrapperMain
import org.zotero.android.sync.LibraryIdentifier
import javax.inject.Inject

val locatorsList = listOf("page", "book", "chapter", "column", "figure", "folio", "issue", "line", "note", "opus", "paragraph", "part", "section", "sub verbo", "verse", "volume")

@HiltViewModel
internal class SingleCitationViewModel @Inject constructor(
    private val dbWrapperMain: DbWrapperMain,
    private val context: Context,
) : BaseViewModel2<SingleCitationViewState, SingleCitationViewEffect>(SingleCitationViewState()) {

    private var libraryId: LibraryIdentifier = LibraryIdentifier.group(0)

    fun init() = initOnce {
        initViewState()
    }

    private fun initViewState() {
        val args = ScreenArguments.singleCitationArgs
        this.libraryId = args.libraryId
        updateState {
            copy(
                selected = args.itemIds.toImmutableSet(),
            )
        }

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
    val selected: ImmutableSet<String> = emptyImmutableSet(),
    var locator: String = locatorsList[0],
    var locatorValue: String = "",
) : ViewState

internal sealed class SingleCitationViewEffect : ViewEffect {
    object OnBack : SingleCitationViewEffect()
}
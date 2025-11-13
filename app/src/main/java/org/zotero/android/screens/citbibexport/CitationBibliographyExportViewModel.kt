package org.zotero.android.screens.citbibexport

import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.architecture.ViewState
import org.zotero.android.database.DbWrapperMain
import javax.inject.Inject

@HiltViewModel
internal class CitationBibliographyExportViewModel @Inject constructor(
    private val dbWrapperMain: DbWrapperMain,
    private val context: Context,
) : BaseViewModel2<CitationBibliographyExportViewState, CitationBibliographyExportViewEffect>(
    CitationBibliographyExportViewState()
) {

    fun init(isTablet: Boolean) = initOnce {
        initViewState()
    }

    private fun initViewState() {
        val args = ScreenArguments.citationBibliographyExportArgs
    }

    fun onDone() {

    }

}

internal data class CitationBibliographyExportViewState(
    val title: String = "",
) : ViewState

internal sealed class CitationBibliographyExportViewEffect : ViewEffect {
    object OnBack : CitationBibliographyExportViewEffect()
}
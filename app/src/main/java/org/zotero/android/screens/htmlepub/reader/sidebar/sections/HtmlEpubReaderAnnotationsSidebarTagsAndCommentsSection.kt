package org.zotero.android.screens.htmlepub.reader.sidebar.sections

import androidx.compose.runtime.Composable
import org.zotero.android.screens.htmlepub.reader.HtmlEpubReaderViewModel
import org.zotero.android.screens.htmlepub.reader.HtmlEpubReaderViewState
import org.zotero.android.screens.htmlepub.reader.data.HtmlEpubAnnotation

@Composable
internal fun HtmlEpubReaderAnnotationsSidebarTagsAndCommentsSection(
    viewState: HtmlEpubReaderViewState,
    viewModel: HtmlEpubReaderViewModel,
    annotation: HtmlEpubAnnotation,
    shouldAddTopPadding: Boolean,
) {
    HtmlEpubReaderAnnotationsSidebarCommentSection(
        annotation = annotation,
        shouldAddTopPadding = shouldAddTopPadding,
        viewModel = viewModel,
        viewState = viewState,
    )
    HtmlEpubReaderAnnotationsSidebarTagsSection(
        annotation = annotation,
        viewModel = viewModel,
        viewState = viewState,
    )
}

package org.zotero.android.screens.reader.sidebar.annotations.sections

import androidx.compose.runtime.Composable
import org.zotero.android.screens.reader.ReaderViewModel
import org.zotero.android.screens.reader.ReaderViewState
import org.zotero.android.screens.reader.data.NewReaderAnnotation

@Composable
internal fun ReaderAnnotationsSidebarTagsAndCommentsSection(
    viewState: ReaderViewState,
    viewModel: ReaderViewModel,
    annotation: NewReaderAnnotation,
    shouldAddTopPadding: Boolean,
) {
    ReaderAnnotationsSidebarCommentSection(
        annotation = annotation,
        shouldAddTopPadding = shouldAddTopPadding,
        viewModel = viewModel,
        viewState = viewState,
    )
    ReaderAnnotationsSidebarTagsSection(
        annotation = annotation,
        viewModel = viewModel,
        viewState = viewState,
    )
}

package org.zotero.android.screens.reader.sidebar.rows

import androidx.compose.runtime.Composable
import org.zotero.android.screens.reader.ReaderViewModel
import org.zotero.android.screens.reader.ReaderViewState
import org.zotero.android.screens.reader.data.NewReaderAnnotation
import org.zotero.android.screens.reader.sidebar.annotations.sections.ReaderAnnotationsSidebarTagsAndCommentsSection

@Composable
internal fun ReaderAnnotationsSidebarNoteRow(
    annotation: NewReaderAnnotation,
    viewModel: ReaderViewModel,
    viewState: ReaderViewState,
) {
    ReaderAnnotationsSidebarTagsAndCommentsSection(
        annotation = annotation,
        viewState = viewState,
        viewModel = viewModel,
        shouldAddTopPadding = true,
    )
}
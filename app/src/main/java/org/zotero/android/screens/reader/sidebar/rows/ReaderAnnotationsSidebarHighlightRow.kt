package org.zotero.android.screens.reader.sidebar.rows

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.zotero.android.screens.reader.ReaderViewModel
import org.zotero.android.screens.reader.ReaderViewState
import org.zotero.android.screens.reader.data.NewReaderAnnotation
import org.zotero.android.screens.reader.sidebar.annotations.sections.ReaderAnnotationsSidebarHighlightedTextSection
import org.zotero.android.screens.reader.sidebar.annotations.sections.ReaderAnnotationsSidebarTagsAndCommentsSection

@Composable
internal fun ReaderAnnotationsSidebarHighlightRow(
    annotation: NewReaderAnnotation,
    viewState: ReaderViewState,
    viewModel: ReaderViewModel,
    annotationColor: Color,
) {
    ReaderAnnotationsSidebarHighlightedTextSection(annotationColor = annotationColor, annotation = annotation)

    ReaderAnnotationsSidebarTagsAndCommentsSection(
        annotation = annotation,
        viewState = viewState,
        viewModel = viewModel,
        shouldAddTopPadding = false,
    )
}
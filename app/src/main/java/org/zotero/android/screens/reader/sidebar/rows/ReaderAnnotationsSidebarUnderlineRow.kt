package org.zotero.android.screens.reader.sidebar.rows

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.zotero.android.screens.reader.ReaderViewModel
import org.zotero.android.screens.reader.ReaderViewState
import org.zotero.android.screens.reader.data.NewReaderAnnotation
import org.zotero.android.screens.reader.sidebar.annotations.sections.ReaderAnnotationsSidebarTagsAndCommentsSection
import org.zotero.android.screens.reader.sidebar.annotations.sections.ReaderAnnotationsSidebarUnderlineTextSection

@Composable
internal fun ReaderAnnotationsSidebarUnderlineRow(
    annotation: NewReaderAnnotation,
    viewState: ReaderViewState,
    viewModel: ReaderViewModel,
    annotationColor: Color,
) {
    ReaderAnnotationsSidebarUnderlineTextSection(annotationColor = annotationColor, annotation = annotation)

    ReaderAnnotationsSidebarTagsAndCommentsSection(
        annotation = annotation,
        viewState = viewState,
        viewModel = viewModel,
        shouldAddTopPadding = false,
    )
}
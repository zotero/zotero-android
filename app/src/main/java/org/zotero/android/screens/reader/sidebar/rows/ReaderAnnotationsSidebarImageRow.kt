package org.zotero.android.screens.reader.sidebar.rows

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import org.zotero.android.screens.reader.ReaderViewModel
import org.zotero.android.screens.reader.ReaderViewState
import org.zotero.android.screens.reader.data.NewReaderAnnotation
import org.zotero.android.screens.reader.sidebar.annotations.sections.ReaderAnnotationsSidebarImageSection
import org.zotero.android.screens.reader.sidebar.annotations.sections.ReaderAnnotationsSidebarTagsAndCommentsSection

@Composable
internal fun ReaderAnnotationsSidebarImageRow(
    viewState: ReaderViewState,
    viewModel: ReaderViewModel,
    annotation: NewReaderAnnotation,
    annotationMaxSideSize: Int,
    cachedBitmap: Bitmap?,
) {
    ReaderAnnotationsSidebarImageSection(
        cachedBitmap = cachedBitmap,
        annotationMaxSideSize = annotationMaxSideSize,
    )
    ReaderAnnotationsSidebarTagsAndCommentsSection(
        viewModel = viewModel,
        viewState = viewState,
        annotation = annotation,
        shouldAddTopPadding = true,
    )
}

package org.zotero.android.screens.reader.sidebar.rows

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import org.zotero.android.screens.reader.ReaderViewModel
import org.zotero.android.screens.reader.ReaderViewState
import org.zotero.android.screens.reader.data.NewReaderAnnotation
import org.zotero.android.screens.reader.sidebar.annotations.sections.ReaderAnnotationsSidebarImageSection
import org.zotero.android.screens.reader.sidebar.annotations.sections.ReaderAnnotationsSidebarTagsSection

@Composable
internal fun ReaderAnnotationsSidebarFreeTextRow(
    viewState: ReaderViewState,
    viewModel: ReaderViewModel,
    annotationMaxSideSize: Int,
    annotation: NewReaderAnnotation,
    cachedBitmap: Bitmap?,
) {
    ReaderAnnotationsSidebarImageSection(cachedBitmap = cachedBitmap, annotationMaxSideSize = annotationMaxSideSize)
    ReaderAnnotationsSidebarTagsSection(viewModel = viewModel, viewState = viewState, annotation = annotation)
}
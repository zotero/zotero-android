package org.zotero.android.screens.htmlepub.reader.sidebar.rows

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import org.zotero.android.screens.htmlepub.reader.HtmlEpubReaderViewModel
import org.zotero.android.screens.htmlepub.reader.HtmlEpubReaderViewState
import org.zotero.android.screens.htmlepub.reader.data.HtmlEpubAnnotation
import org.zotero.android.screens.htmlepub.reader.sidebar.annotations.sections.HtmlEpubReaderAnnotationsSidebarImageSection
import org.zotero.android.screens.htmlepub.reader.sidebar.annotations.sections.HtmlEpubReaderAnnotationsSidebarTagsSection

@Composable
internal fun HtmlEpubReaderAnnotationsSidebarInkRow(
    viewState: HtmlEpubReaderViewState,
    viewModel: HtmlEpubReaderViewModel,
    annotation: HtmlEpubAnnotation,
    annotationMaxSideSize: Int,
    cachedBitmap: Bitmap?,
) {
    HtmlEpubReaderAnnotationsSidebarImageSection(
        cachedBitmap = cachedBitmap,
        annotationMaxSideSize = annotationMaxSideSize
    )
    HtmlEpubReaderAnnotationsSidebarTagsSection(
        viewModel = viewModel,
        viewState = viewState,
        annotation = annotation
    )
}
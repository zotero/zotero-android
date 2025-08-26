package org.zotero.android.pdf.annotation.row

import androidx.compose.runtime.Composable
import org.zotero.android.pdf.annotation.blocks.PdfAnnotationSizeSelector
import org.zotero.android.pdf.annotation.sections.PdfAnnotationCommentSection
import org.zotero.android.pdf.annotation.sections.PdfAnnotationTagsSection
import org.zotero.android.pdf.data.PDFAnnotation
import org.zotero.android.screens.settings.elements.NewSettingsDivider
import org.zotero.android.sync.Tag

@Composable
internal fun PdfAnnotationInkRow(
    annotation: PDFAnnotation,
    commentFocusText: String,
    onCommentTextChange: (String) -> Unit,
    tags: List<Tag>,
    onTagsClicked: () -> Unit,
    size: Float,
    onSizeChanged: (Float) -> Unit,
) {
    PdfAnnotationCommentSection(
        annotation = annotation,
        commentFocusText = commentFocusText,
        onCommentTextChange = onCommentTextChange,
    )
    if (annotation.isZoteroAnnotation) {
        NewSettingsDivider()
        PdfAnnotationSizeSelector(
            size = size,
            onSizeChanged = onSizeChanged,
        )
        NewSettingsDivider()
        PdfAnnotationTagsSection(
            tags = tags,
            onTagsClicked = onTagsClicked,
        )
    }
}


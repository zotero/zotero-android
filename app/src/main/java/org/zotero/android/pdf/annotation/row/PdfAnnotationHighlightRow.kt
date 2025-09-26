package org.zotero.android.pdf.annotation.row

import androidx.compose.runtime.Composable
import org.zotero.android.pdf.annotation.blocks.PdfAnnotationColorPicker
import org.zotero.android.pdf.annotation.sections.PdfAnnotationCommentSection
import org.zotero.android.pdf.annotation.sections.PdfAnnotationTagsSection
import org.zotero.android.pdf.data.PDFAnnotation
import org.zotero.android.screens.settings.elements.NewSettingsDivider
import org.zotero.android.sync.Tag

@Composable
internal fun PdfAnnotationHighlightRow(
    annotation: PDFAnnotation,
    commentFocusText: String,
    onCommentTextChange: (String) -> Unit,
    colors: List<String>,
    onColorSelected: (color: String) -> Unit,
    selectedColor: String,
    tags: List<Tag>,
    onTagsClicked: () -> Unit
) {
    PdfAnnotationCommentSection(
        annotation = annotation,
        commentFocusText = commentFocusText,
        onCommentTextChange = onCommentTextChange,
    )
    if (annotation.isZoteroAnnotation) {
        NewSettingsDivider()
        PdfAnnotationColorPicker(
            colors = colors,
            onColorSelected = onColorSelected,
            selectedColor = selectedColor,
        )
        NewSettingsDivider()
        PdfAnnotationTagsSection(
            tags = tags,
            onTagsClicked = onTagsClicked,
        )
    }
}

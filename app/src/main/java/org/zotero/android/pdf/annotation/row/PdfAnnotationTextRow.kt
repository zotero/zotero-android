package org.zotero.android.pdf.annotation.row

import androidx.compose.runtime.Composable
import org.zotero.android.pdf.annotation.blocks.PdfAnnotationColorPicker
import org.zotero.android.pdf.annotation.blocks.PdfAnnotationFontSizeSelector
import org.zotero.android.pdf.annotation.sections.PdfAnnotationTagsSection
import org.zotero.android.pdf.data.PDFAnnotation
import org.zotero.android.screens.settings.elements.NewSettingsDivider
import org.zotero.android.sync.Tag

@Composable
internal fun PdfAnnotationTextRow(
    annotation: PDFAnnotation,
    fontSize: Float,
    onFontSizeDecrease: () -> Unit,
    onFontSizeIncrease: () -> Unit,
    onColorSelected: (String) -> Unit,
    colors: List<String>,
    selectedColor: String,
    tags: List<Tag>,
    onTagsClicked: () -> Unit,
) {
    if (annotation.isZoteroAnnotation) {
        PdfAnnotationFontSizeSelector(
            fontSize = fontSize,
            onFontSizeDecrease = onFontSizeDecrease,
            onFontSizeIncrease = onFontSizeIncrease,
        )
        NewSettingsDivider()
        PdfAnnotationColorPicker(
            colors = colors,
            onColorSelected = onColorSelected,
            selectedColor = selectedColor
        )
        NewSettingsDivider()
        PdfAnnotationTagsSection(
            tags = tags,
            onTagsClicked = onTagsClicked,
        )
    }
}


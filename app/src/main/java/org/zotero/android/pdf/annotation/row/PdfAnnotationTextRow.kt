package org.zotero.android.pdf.annotation.row

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.pdf.annotation.ColorPicker
import org.zotero.android.pdf.annotation.FontSizeSelector
import org.zotero.android.pdf.annotation.TagsSection
import org.zotero.android.pdf.annotationmore.SpacerDivider
import org.zotero.android.pdf.data.PDFAnnotation
import org.zotero.android.sync.Tag

@Composable
internal fun PdfAnnotationTextRow(
    annotation: PDFAnnotation,
    layoutType: CustomLayoutSize.LayoutType,
    fontSize: Float,
    onFontSizeDecrease: () -> Unit,
    onFontSizeIncrease: () -> Unit,
    onColorSelected: (String) -> Unit,
    colors: List<String>,
    selectedColor: String,
    tags: List<Tag>,
    onTagsClicked: () -> Unit,
    ) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 0.dp, bottom = 8.dp)
    ) {
        if (annotation.isZoteroAnnotation) {
            SpacerDivider()
            FontSizeSelector(
                fontSize = fontSize,
                onFontSizeDecrease = onFontSizeDecrease,
                onFontSizeIncrease = onFontSizeIncrease,
            )
            SpacerDivider()
            ColorPicker(
                colors = colors,
                onColorSelected = onColorSelected,
                selectedColor = selectedColor
            )
            SpacerDivider()
            Spacer(modifier = Modifier.height(4.dp))
            TagsSection(
                tags = tags,
                onTagsClicked = onTagsClicked,
                layoutType = layoutType
            )
            Spacer(modifier = Modifier.height(4.dp))
            SpacerDivider()
        }
    }
}


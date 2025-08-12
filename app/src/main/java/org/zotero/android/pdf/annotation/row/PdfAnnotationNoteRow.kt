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
import org.zotero.android.pdf.annotation.CommentSection
import org.zotero.android.pdf.annotation.TagsSection
import org.zotero.android.pdf.data.PDFAnnotation
import org.zotero.android.pdf.reader.sidebar.SidebarDivider
import org.zotero.android.sync.Tag

@Composable
internal fun PdfAnnotationNoteRow(
    layoutType: CustomLayoutSize.LayoutType,
    annotation: PDFAnnotation,
    colors: List<String>,
    onColorSelected: (color: String) -> Unit,
    selectedColor: String,
    tags: List<Tag>,
    onTagsClicked: () -> Unit,
    commentFocusText: String,
    onCommentTextChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 0.dp, bottom = 8.dp)
    ) {
        CommentSection(
            annotation = annotation,
            layoutType = layoutType,
            commentFocusText = commentFocusText,
            onCommentTextChange = onCommentTextChange,
        )
        Spacer(modifier = Modifier.height(4.dp))

        if (annotation.isZoteroAnnotation) {
            SidebarDivider()
            Spacer(modifier = Modifier.height(4.dp))
            ColorPicker(
                colors = colors,
                onColorSelected = onColorSelected,
                selectedColor = selectedColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            SidebarDivider()
            Spacer(modifier = Modifier.height(4.dp))
            TagsSection(
                layoutType = layoutType,
                tags = tags,
                onTagsClicked = onTagsClicked,
            )
        }

    }
}

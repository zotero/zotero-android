package org.zotero.android.pdf.annotation.sections

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.zotero.android.pdf.data.PDFAnnotation
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.textinput.CustomTextField

@Composable
internal fun PdfAnnotationCommentSection(
    annotation: PDFAnnotation,
    commentFocusText: String,
    onCommentTextChange: (String) -> Unit,
) {
    val enabled = annotation.isZoteroAnnotation
    CustomTextField(
        modifier = Modifier
            .padding(horizontal = 16.dp),
        value = commentFocusText,
        textStyle = MaterialTheme.typography.bodyMedium,
        textColor = MaterialTheme.colorScheme.onSurface,
        hintColor = MaterialTheme.colorScheme.primary,
        hintTextStyle = MaterialTheme.typography.labelLarge,
        hint = if (enabled) {
            stringResource(id = Strings.pdf_annotations_sidebar_add_comment)
        } else {
            stringResource(id = Strings.pdf_annotation_popover_no_comment)
        },
        ignoreTabsAndCaretReturns = false,
        minLines = 5,
        enabled = enabled,
        onValueChange = onCommentTextChange
    )
}

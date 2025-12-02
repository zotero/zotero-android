package org.zotero.android.pdf.reader.sidebar.sections

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.zotero.android.pdf.data.PDFAnnotation
import org.zotero.android.pdf.reader.PdfReaderVMInterface
import org.zotero.android.pdf.reader.PdfReaderViewState
import org.zotero.android.pdf.reader.sidebar.sectionHorizontalPadding
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.textinput.CustomTextField

@Composable
internal fun PdfReaderAnnotationsSidebarCommentSection(
    vMInterface: PdfReaderVMInterface,
    viewState: PdfReaderViewState,
    annotation: PDFAnnotation,
    shouldAddTopPadding: Boolean,
) {
    if (viewState.isAnnotationSelected(annotation.key) && annotation.isZoteroAnnotation) {
        CustomTextField(
            modifier = Modifier
                .sectionHorizontalPadding()
                .padding(bottom = 8.dp)
                .padding(top = if (shouldAddTopPadding) 8.dp else 0.dp)
                .onFocusChanged {
                    if (it.hasFocus) {
                        vMInterface.onCommentFocusFieldChange(annotation.key)
                    }
                },
            value = if (annotation.key == viewState.commentFocusKey) {
                viewState.commentFocusText
            } else {
                annotation.comment
            },
            textStyle = MaterialTheme.typography.bodyMedium,
            textColor = MaterialTheme.colorScheme.onSurface,
            hint = stringResource(id = Strings.pdf_annotations_sidebar_add_comment),
            hintColor = MaterialTheme.colorScheme.primary,
            hintTextStyle = MaterialTheme.typography.labelLarge,
            ignoreTabsAndCaretReturns = false,
            onValueChange = { vMInterface.onCommentTextChange(annotationKey = annotation.key, it) })
    } else if (annotation.comment.isNotBlank()) {
        Text(
            modifier = Modifier
                .sectionHorizontalPadding()
                .padding(bottom = 8.dp)
                .padding(top = if (shouldAddTopPadding) 8.dp else 0.dp),
            text = annotation.comment,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
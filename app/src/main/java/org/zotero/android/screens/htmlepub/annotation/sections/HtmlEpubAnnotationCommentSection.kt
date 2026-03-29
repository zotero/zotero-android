package org.zotero.android.screens.htmlepub.annotation.sections

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.zotero.android.screens.htmlepub.reader.data.HtmlEpubAnnotation
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.textinput.CustomTextField

@Composable
internal fun HtmlEpubAnnotationCommentSection(
    annotation: HtmlEpubAnnotation,
    commentFocusText: String,
    onCommentTextChange: (String) -> Unit,
) {
    CustomTextField(
        modifier = Modifier
            .padding(horizontal = 16.dp),
        value = commentFocusText,
        textStyle = MaterialTheme.typography.bodyMedium,
        textColor = MaterialTheme.colorScheme.onSurface,
        hintColor = MaterialTheme.colorScheme.primary,
        hintTextStyle = MaterialTheme.typography.labelLarge,
        hint = stringResource(id = Strings.pdf_annotations_sidebar_add_comment)
        ,
        ignoreTabsAndCaretReturns = false,
        minLines = 5,
        onValueChange = onCommentTextChange
    )
}

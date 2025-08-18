package org.zotero.android.pdf.annotation

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.pdf.data.PDFAnnotation
import org.zotero.android.sync.Tag
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.debounceClickable
import org.zotero.android.uicomponents.textinput.CustomTextField
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun CommentSection(
    layoutType: CustomLayoutSize.LayoutType,
    annotation: PDFAnnotation,
    commentFocusText: String,
    onCommentTextChange: (String) -> Unit,
) {
    val enabled = annotation.isZoteroAnnotation
    CustomTextField(
        modifier = Modifier
            .padding(start = 8.dp),
        value = commentFocusText,
        textStyle = CustomTheme.typography.default.copy(fontSize = layoutType.calculatePdfSidebarTextSize()),
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

@Composable
internal fun TagsSection(
    layoutType: CustomLayoutSize.LayoutType,
    tags: List<Tag>,
    onTagsClicked: () -> Unit
) {
    if (!tags.isEmpty()) {
        Text(
            modifier = Modifier
                .padding(start = 16.dp)
                .debounceClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onTagsClicked
                ),
            text = tags.joinToString(separator = ", ") { it.name },
            color = CustomTheme.colors.primaryContent,
            style = CustomTheme.typography.default,
            fontSize = layoutType.calculatePdfSidebarTextSize(),
        )
    } else {
        Text(
            modifier = Modifier
                .padding(start = 8.dp)
                .debounceClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onTagsClicked
                ),
            text = stringResource(id = Strings.pdf_annotations_sidebar_add_tags),
            color = CustomTheme.colors.zoteroDefaultBlue,
            style = CustomTheme.typography.default,
            fontSize = layoutType.calculatePdfSidebarTextSize(),
        )
    }
}
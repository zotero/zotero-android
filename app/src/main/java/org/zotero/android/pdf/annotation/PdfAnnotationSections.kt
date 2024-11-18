package org.zotero.android.pdf.annotation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.textinput.CustomTextField
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun CommentSection(
    viewState: PdfAnnotationViewState,
    layoutType: CustomLayoutSize.LayoutType,
    viewModel: PdfAnnotationViewModel
) {
    CustomTextField(
        modifier = Modifier
            .padding(start = 8.dp),
        value = viewState.commentFocusText,
        textStyle = CustomTheme.typography.default.copy(fontSize = layoutType.calculatePdfSidebarTextSize()),
        hint = stringResource(id = Strings.pdf_annotations_sidebar_add_comment),
        ignoreTabsAndCaretReturns = false,
        minLines = 5,
        onValueChange = { viewModel.onCommentTextChange(it) })
}

@Composable
internal fun TagsSection(
    viewModel: PdfAnnotationViewModel,
    viewState: PdfAnnotationViewState,
    layoutType: CustomLayoutSize.LayoutType
) {
    if (!viewState.tags.isEmpty()) {
        Text(
            modifier = Modifier
                .padding(start = 16.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { viewModel.onTagsClicked() }
                ),
            text = viewState.tags.joinToString(separator = ", ") { it.name },
            color = CustomTheme.colors.primaryContent,
            style = CustomTheme.typography.default,
            fontSize = layoutType.calculatePdfSidebarTextSize(),
        )
    } else {
        Text(
            modifier = Modifier
                .padding(start = 8.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { viewModel.onTagsClicked() }
                ),
            text = stringResource(id = Strings.pdf_annotations_sidebar_add_tags),
            color = CustomTheme.colors.zoteroDefaultBlue,
            style = CustomTheme.typography.default,
            fontSize = layoutType.calculatePdfSidebarTextSize(),
        )
    }
}
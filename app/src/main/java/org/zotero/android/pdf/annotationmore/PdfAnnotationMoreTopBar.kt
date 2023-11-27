package org.zotero.android.pdf.annotationmore

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.topbar.NewCustomTopBar
import org.zotero.android.uicomponents.topbar.NewHeadingTextButton

@Composable
internal fun PdfAnnotationMoreTopBar(
    onBack: () -> Unit,
    viewModel: PdfAnnotationMoreViewModel
) {
    NewCustomTopBar(
        title = stringResource(id = Strings.pdf_annotation_popover_title),
        backgroundColor = CustomTheme.colors.zoteroEditFieldBackground,
        leftContainerContent = listOf {
            NewHeadingTextButton(text = stringResource(id = Strings.cancel), onClick = onBack)
        },
        rightContainerContent = listOf {
            NewHeadingTextButton(
                text = stringResource(id = Strings.save),
                onClick = viewModel::onSave
            )
        }
    )
}
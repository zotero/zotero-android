package org.zotero.android.pdf.annotationmore.editpage

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.topbar.NewCustomTopBar
import org.zotero.android.uicomponents.topbar.NewHeadingTextButton

@Composable
internal fun PdfAnnotationEditPageTopBar(viewModel: PdfAnnotationEditPageViewModel) {
    NewCustomTopBar(
        backgroundColor = CustomTheme.colors.zoteroEditFieldBackground,
        title = stringResource(id = Strings.pdf_annotation_popover_page_label_title),
        leftContainerContent = listOf {
            NewHeadingTextButton(
                text = stringResource(id = Strings.back_button),
                onClick = viewModel::onBack
            )
        }
    )
}
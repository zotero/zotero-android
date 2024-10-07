package org.zotero.android.pdf.pdffilter

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.topbar.NewCustomTopBar
import org.zotero.android.uicomponents.topbar.NewHeadingTextButton

@Composable
internal fun PdfFilterScreenTopBar(
    onClose: () -> Unit,
    onClear: (() -> Unit)?,
) {
    NewCustomTopBar(
        title = stringResource(id = Strings.pdf_annotations_sidebar_filter_title),
        leftContainerContent = listOf {
            NewHeadingTextButton(
                onClick = onClose,
                text = stringResource(Strings.close),
            )
        },
        rightContainerContent = listOf {
            if (onClear != null) {
                NewHeadingTextButton(
                    onClick = onClear,
                    text = stringResource(Strings.clear),
                )
            }
        }
    )
}

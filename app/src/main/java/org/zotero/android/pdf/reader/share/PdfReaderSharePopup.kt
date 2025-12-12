package org.zotero.android.pdf.reader.share

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import org.zotero.android.pdf.reader.PdfReaderVMInterface
import org.zotero.android.pdf.reader.PdfReaderViewState
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings

@Composable
internal fun PdfReaderSharePopup(
    viewState: PdfReaderViewState,
    viewModel: PdfReaderVMInterface,
) {
    Popup(
        properties = PopupProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        ),
        onDismissRequest = viewModel::dismissSharePopup,
        popupPositionProvider = pdfReaderSharePopupPositionProvider(),
    ) {
        Column(
            modifier = Modifier
                .width(250.dp)
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(16.dp),
                )
                .background(color = MaterialTheme.colorScheme.surface)
        ) {
            if (viewState.parentKey != null) {
                PdfReaderSharePopupOptionRow(
                    text = stringResource(id = Strings.citation_copy_citation),
                    onOptionClick = viewModel::onCopyCitation,
                    resIcon = Drawables.file_copy
                )
                PdfReaderSharePopupOptionRow(
                    text = stringResource(id = Strings.citation_copy_bibliography),
                    onOptionClick = viewModel::onCopyBibliography,
                    resIcon = Drawables.file_copy
                )
            }
            PdfReaderSharePopupOptionRow(
                text = stringResource(id = Strings.pdf_export_export),
                onOptionClick = viewModel::onExportPdf,
                resIcon = Drawables.share_24
            )
            PdfReaderSharePopupOptionRow(
                text = stringResource(id = Strings.pdf_export_export_annotated),
                onOptionClick = viewModel::onExportAnnotatedPdf,
                resIcon = Drawables.share_24
            )
        }
    }
}
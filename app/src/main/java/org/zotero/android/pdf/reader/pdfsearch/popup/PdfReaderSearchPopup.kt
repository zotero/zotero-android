package org.zotero.android.pdf.reader.pdfsearch.popup

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import org.zotero.android.pdf.reader.PdfReaderVMInterface
import org.zotero.android.pdf.reader.pdfsearch.PdfReaderSearchScreen
import org.zotero.android.pdf.reader.pdfsearch.PdfReaderSearchViewModel
import org.zotero.android.pdf.reader.pdfsearch.PdfReaderSearchViewState
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun PdfReaderSearchPopup(
    viewModel: PdfReaderVMInterface,
    pdfReaderSearchViewModel: PdfReaderSearchViewModel,
    pdfReaderSearchViewState: PdfReaderSearchViewState,
) {
    val backgroundColor = CustomTheme.colors.cardBackground
    Popup(
        properties = PopupProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            focusable = true
        ),
        onDismissRequest = viewModel::hidePdfSearch,
        popupPositionProvider = pdfReaderSearchPopupPositionProvider(),

        ) {
        CustomScaffold(
            modifier = Modifier
                .width(350.dp)
                .height(530.dp)
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(16.dp),
                ),
            topBarColor = backgroundColor,
        ) {
            PdfReaderSearchScreen(
                onBack = viewModel::hidePdfSearch,
                viewModel = pdfReaderSearchViewModel,
                viewState = pdfReaderSearchViewState
            )
        }
    }
}

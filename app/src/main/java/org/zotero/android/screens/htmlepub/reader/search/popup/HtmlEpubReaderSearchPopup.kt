package org.zotero.android.screens.htmlepub.reader.search.popup

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import org.zotero.android.screens.htmlepub.reader.HtmlEpubReaderViewModel
import org.zotero.android.screens.htmlepub.reader.search.HtmlEpubReaderSearchScreen
import org.zotero.android.screens.htmlepub.reader.search.HtmlEpubReaderSearchViewModel
import org.zotero.android.screens.htmlepub.reader.search.HtmlEpubReaderSearchViewState
import org.zotero.android.uicomponents.CustomScaffoldM3

@Composable
internal fun HtmlEpubReaderSearchPopup(
    viewModel: HtmlEpubReaderViewModel,
    htmlEpubReaderSearchViewModel: HtmlEpubReaderSearchViewModel,
    htmlEpubReaderSearchViewState: HtmlEpubReaderSearchViewState,
) {
    Popup(
        properties = PopupProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            focusable = true
        ),
        onDismissRequest = viewModel::hidePdfSearch,
        popupPositionProvider = htmlEpubReaderSearchPopupPositionProvider(),

        ) {
        CustomScaffoldM3(
            modifier = Modifier
                .width(350.dp)
                .height(530.dp)
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(16.dp),
                ),
            topBar = {},
        ) {
            HtmlEpubReaderSearchScreen(
                onBack = viewModel::hidePdfSearch,
                viewModel = htmlEpubReaderSearchViewModel,
                viewState = htmlEpubReaderSearchViewState
            )
        }
    }
}

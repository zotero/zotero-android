package org.zotero.android.screens.reader.search.popup

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import org.zotero.android.screens.reader.ReaderViewModel
import org.zotero.android.screens.reader.search.ReaderSearchScreen
import org.zotero.android.screens.reader.search.ReaderSearchViewModel
import org.zotero.android.screens.reader.search.ReaderSearchViewState
import org.zotero.android.uicomponents.CustomScaffoldM3

@Composable
internal fun ReaderSearchPopup(
    viewModel: ReaderViewModel,
    readerSearchViewModel: ReaderSearchViewModel,
    readerSearchViewState: ReaderSearchViewState,
) {
    Popup(
        properties = PopupProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            focusable = true
        ),
        onDismissRequest = viewModel::hidePdfSearch,
        popupPositionProvider = readerSearchPopupPositionProvider(),

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
            ReaderSearchScreen(
                onBack = viewModel::hidePdfSearch,
                viewModel = readerSearchViewModel,
                viewState = readerSearchViewState
            )
        }
    }
}

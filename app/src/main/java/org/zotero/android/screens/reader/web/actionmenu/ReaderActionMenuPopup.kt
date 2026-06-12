package org.zotero.android.screens.reader.web.actionmenu

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.google.gson.JsonArray
import org.zotero.android.screens.reader.ReaderViewModel

@Composable
fun ReaderActionMenuPopup(
    selectedTextParamsRects: JsonArray,
    viewModel: ReaderViewModel,
) {
    val onDismiss: () -> Unit = {
        viewModel.dismissActionMenu()
    }

    Popup(
        properties = PopupProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        ),
        onDismissRequest = onDismiss,
        popupPositionProvider = readerActionMenuPopupPositionProvider(selectedTextParamsRects),
    ) {
        ReaderActionMenu(viewModel)
    }
}
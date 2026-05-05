package org.zotero.android.screens.htmlepub.reader.web.actionmenu

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.google.gson.JsonArray
import org.zotero.android.screens.htmlepub.reader.HtmlEpubReaderViewModel

@Composable
fun HtmlEpubActionMenuPopup(
    selectedTextParamsRects: JsonArray,
    viewModel: HtmlEpubReaderViewModel,
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
        popupPositionProvider = htmlEpubActionMenuPopupPositionProvider(selectedTextParamsRects),
    ) {
        HtmlEpubActionMenu(viewModel)
    }
}
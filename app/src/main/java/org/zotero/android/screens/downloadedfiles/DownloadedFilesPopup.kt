package org.zotero.android.screens.downloadedfiles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import org.zotero.android.screens.allitems.AllItemsViewModel
import org.zotero.android.screens.allitems.AllItemsViewState
import org.zotero.android.uicomponents.CustomScaffoldM3
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.controls.CustomSwitch

@Composable
internal fun DownloadedFilesPopup(
    viewState: AllItemsViewState,
    viewModel: AllItemsViewModel,
) {
    Popup(
        properties = PopupProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        ),
        onDismissRequest = viewModel::dismissDownloadedFilesPopup,
        popupPositionProvider = downloadedFilesPopupPositionProvider(),
    ) {
        CustomScaffoldM3(
            modifier = Modifier
                .width(350.dp)
                .height(120.dp)
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(16.dp),
                ),
            topBar = {
                DownloadedFilesTopBar(
                    onDone = viewModel::dismissDownloadedFilesPopup,
                )
            },
        ) {
            Row(
                modifier = Modifier
                    .height(48.dp)
                    .background(color = MaterialTheme.colorScheme.surface),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    modifier = Modifier.weight(1f),
                    text = stringResource(id = Strings.items_filters_downloads),
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge,
                )
                CustomSwitch(
                    checked = viewState.isDownloadsFilterEnabled(),
                    onCheckedChange = { viewModel.onDownloadedFilesTapped() },
                    modifier = Modifier
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
        }

    }
}


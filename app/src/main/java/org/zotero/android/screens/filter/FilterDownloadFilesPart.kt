package org.zotero.android.screens.filter

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.controls.CustomSwitch
import org.zotero.android.uicomponents.misc.CustomDivider
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun DownloadFilesPart(
    viewState: FilterViewState,
    viewModel: FilterViewModel
) {
    Row(
        modifier = Modifier.padding(top = 4.dp, start = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(id = Strings.items_filters_downloads),
            maxLines = 1,
            style = CustomTheme.typography.newBody,
        )
        CustomSwitch(
            checked = viewState.isDownloadsChecked,
            onCheckedChange = { viewModel.onDownloadsTapped() },
            modifier = Modifier
        )
        Spacer(modifier = Modifier.width(8.dp))
    }
    CustomDivider()
}
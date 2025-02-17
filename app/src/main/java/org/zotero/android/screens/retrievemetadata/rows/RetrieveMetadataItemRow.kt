package org.zotero.android.screens.retrievemetadata.rows

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.zotero.android.screens.retrievemetadata.RetrieveMetadataViewState
import org.zotero.android.uicomponents.misc.CustomDivider

@Composable
internal fun RetrieveMetadataItemRow(
    viewState: RetrieveMetadataViewState,
    showBottomDivider: Boolean = false,
) {
    val rowModifier: Modifier = Modifier.height(64.dp)
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = rowModifier
        ) {
            RetrieveMetadataItemRowLeftPart()
            Spacer(modifier = Modifier.width(16.dp))
            RetrieveMetadataItemRowCentralPart(
                title = viewState.pdfFileName,
                retrieveMetadataState = viewState.retrieveMetadataState
            )
        }
        if (showBottomDivider) {
            CustomDivider(modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 60.dp))
        }
    }
}
package org.zotero.android.screens.share.sections

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.zotero.android.database.objects.ItemTypes
import org.zotero.android.screens.retrievemetadata.data.RetrieveMetadataState
import org.zotero.android.screens.share.rows.ShareRecognizedItemRow

@Composable
internal fun ShareRecognizeItemSection(retrieveMetadataState: RetrieveMetadataState.success) {
    ShareRecognizedItemRow(
        title = retrieveMetadataState.recognizedTitle,
        iconSize = 28.dp,
        typeIconName = retrieveMetadataState.recognizedTypeIcon
    )
    Row {
        Spacer(modifier = Modifier.width(16.dp))
        ShareRecognizedItemRow(
            title = "PDF",
            iconSize = 22.dp,
            typeIconName = ItemTypes.iconName(
                rawType = ItemTypes.attachment,
                contentType = "pdf"
            )
        )
    }
}

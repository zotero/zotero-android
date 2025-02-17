package org.zotero.android.screens.retrievemetadata.rows

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.Drawables

@Composable
internal fun RetrieveMetadataItemRowLeftPart(
) {
    Spacer(modifier = Modifier.width(16.dp))
    Image(
        modifier = Modifier.size(28.dp),
        painter = painterResource(id = Drawables.item_type_pdf),
        contentDescription = null,
    )
}
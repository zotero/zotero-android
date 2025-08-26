package org.zotero.android.screens.itemdetails.rows

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.zotero.android.screens.itemdetails.ItemDetailHeaderSection
import org.zotero.android.uicomponents.Strings

@Composable
internal fun ItemDetailsAbstractFieldRow(
    detailValue: String,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        ItemDetailHeaderSection(Strings.abstract_1)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = detailValue,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
        )
    }

}
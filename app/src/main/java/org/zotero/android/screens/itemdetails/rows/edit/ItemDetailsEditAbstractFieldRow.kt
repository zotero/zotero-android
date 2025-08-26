package org.zotero.android.screens.itemdetails.rows.edit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.zotero.android.screens.itemdetails.ItemDetailHeaderSection
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.textinput.CustomTextField

@Composable
internal fun ItemDetailsEditAbstractFieldRow(
    detailValue: String,
    onValueChange: (String) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        ItemDetailHeaderSection(Strings.abstract_1)
        Spacer(modifier = Modifier.height(8.dp))
        CustomTextField(
            modifier = Modifier
                .fillMaxSize(),
            value = detailValue,
            hint = "",
            textColor = MaterialTheme.colorScheme.onSurface,
            textStyle = MaterialTheme.typography.bodyLarge,
            onValueChange = onValueChange,
            ignoreTabsAndCaretReturns = false,
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}
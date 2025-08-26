package org.zotero.android.screens.tagpicker.row

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.safeClickable

@Composable
internal fun TagPickerCreateTagRow(
    tagName: String,
    onTap: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .safeClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onTap
            ),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            modifier = Modifier.padding(start = 16.dp),
            text = stringResource(id = Strings.tag_picker_create_tag, tagName),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
        )
    }

}

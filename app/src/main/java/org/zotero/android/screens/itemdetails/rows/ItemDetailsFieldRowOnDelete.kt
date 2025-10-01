package org.zotero.android.screens.itemdetails.rows

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.foundation.safeClickable

@Composable
internal fun ItemDetailsFieldRowOnDelete(onDelete: (() -> Unit)?) {
    if (onDelete != null) {
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            modifier = Modifier
                .size(28.dp)
                .safeClickable(
                    onClick = onDelete,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false)
                )
                .padding(start = 4.dp),
            painter = painterResource(id = Drawables.do_not_disturb_on_24px),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.width(8.dp))
    }
}
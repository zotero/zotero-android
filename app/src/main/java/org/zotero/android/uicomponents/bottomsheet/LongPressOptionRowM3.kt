package org.zotero.android.uicomponents.bottomsheet

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
fun LongPressOptionRowM3(
    optionItem: LongPressOptionItem,
    onOptionClick: (LongPressOptionItem) -> Unit
) {
    val color = if (optionItem.isEnabled) {
        optionItem.textAndIconColor ?: MaterialTheme.colorScheme.onSurface
    } else {
        CustomTheme.colors.disabledContent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(48.dp)
            .safeClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = { onOptionClick(optionItem) },
            ), verticalAlignment = Alignment.CenterVertically
    ) {
        if (optionItem.resIcon != null) {
            Spacer(modifier = Modifier.width(16.dp))
            Icon(
                painter = painterResource(optionItem.resIcon),
                contentDescription = null,
                tint = color,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = stringResource(id = optionItem.titleId),
            color = color,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.width(16.dp))

    }
}
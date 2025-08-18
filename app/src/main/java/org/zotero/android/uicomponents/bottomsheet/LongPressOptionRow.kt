package org.zotero.android.uicomponents.bottomsheet

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.row.BaseRowItemWithIcon
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
fun LongPressOptionRow(
    optionItem: LongPressOptionItem,
    onOptionClick: (LongPressOptionItem) -> Unit
) {
    val color = if (optionItem.isEnabled) {
        optionItem.textAndIconColor ?: CustomTheme.colors.zoteroDefaultBlue
    } else {
        CustomTheme.colors.disabledContent
    }

    BaseRowItemWithIcon(
        title = stringResource(id = optionItem.titleId),
        textColor = color,
        titleStyle = CustomTheme.typography.newBody,
        heightIn = if (optionItem.resIcon == null) 32.dp else 64.dp,
        onClick = { onOptionClick(optionItem) },
    ) {
        if (optionItem.resIcon != null) {
            Icon(
                painter = painterResource(optionItem.resIcon),
                contentDescription = null,
                modifier = Modifier.padding(start = 8.dp),
                tint = color,
            )
        }
    }
}
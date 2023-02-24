package org.zotero.android.uicomponents.bottomsheet

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
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
    val color = optionItem.textAndIconColor ?: CustomTheme.colors.primaryContent
    BaseRowItemWithIcon(
        title = stringResource(id = optionItem.titleId),
        textColor = color,
        titleStyle = CustomTheme.typography.default,
        onClick = { onOptionClick(optionItem) },
    ) {
        Icon(
            painter = painterResource(optionItem.resIcon),
            contentDescription = null,
            modifier = Modifier.padding(start = 8.dp),
            tint = color,
        )
    }
}
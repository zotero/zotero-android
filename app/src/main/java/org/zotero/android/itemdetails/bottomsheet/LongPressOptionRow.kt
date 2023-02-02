package org.zotero.android.itemdetails.bottomsheet

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
    BaseRowItemWithIcon(
        title = stringResource(id = optionItem.titleId),
        textColor = optionItem.textAndIconColor,
        titleStyle = CustomTheme.typography.default,
        onClick = { onOptionClick(optionItem) },
    ) {
        Icon(
            painter = painterResource(optionItem.resIcon),
            contentDescription = null,
            modifier = Modifier.padding(start = 8.dp),
            tint = optionItem.textAndIconColor,
        )
    }
}
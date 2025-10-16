package org.zotero.android.uicomponents.misc

import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
fun CustomDivider(
    modifier: Modifier = Modifier
) {
    Divider(
        modifier = modifier,
        color = CustomTheme.colors.divider,
        thickness = 1.dp
    )
}

@Composable
fun NewDivider(
    modifier: Modifier = Modifier
) {
    Divider(
        modifier = modifier,
        color = CustomTheme.colors.newDividerColor,
        thickness = 1.dp
    )
}
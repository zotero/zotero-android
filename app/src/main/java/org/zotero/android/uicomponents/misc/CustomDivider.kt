package org.zotero.android.uicomponents.misc

import androidx.compose.material.Divider
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

/**
 * This one is used for visual separation between surface sections when it's not
 * possible to just use surfaces placed above the background. E.g. in scrolling
 * content when the surface needs to fill the whole screen.
 */
@Composable
fun CustomBackgroundDivider(
    modifier: Modifier = Modifier
) {
    Divider(
        modifier = modifier,
        color = CustomTheme.colors.windowBackground,
        thickness = 8.dp
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

@Composable
fun PopupDivider(
    modifier: Modifier = Modifier
) {
    Divider(
        modifier = modifier,
        color = CustomTheme.colors.popupDividerColor,
        thickness = 1.dp
    )
}

package org.zotero.android.uicomponents.topbar

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
fun CloseIconTopBar(
    titleResId: Int,
    elevation: Dp = AppBarDefaults.TopAppBarElevation,
    onClose: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    CloseIconTopBar(
        title = stringResource(id = titleResId),
        onClose = onClose,
        actions = actions,
        elevation = elevation
    )
}

@Composable
fun CloseIconTopBar(
    title: String?,
    onClose: () -> Unit = {},
    elevation: Dp = AppBarDefaults.TopAppBarElevation,
    backgroundColor: Color = CustomTheme.colors.surface,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = {
            if (title != null) {
                Text(
                    text = title,
                    color = CustomTheme.colors.primaryContent,
                    style = CustomTheme.typography.h2
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "",
                )
            }
        },
        actions = actions,
        backgroundColor = backgroundColor,
        elevation = elevation,
    )
}

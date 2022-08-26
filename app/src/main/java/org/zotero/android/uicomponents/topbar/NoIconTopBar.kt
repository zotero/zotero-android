package org.zotero.android.uicomponents.topbar

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
fun NoIconTopBar(
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
fun NoIconTopBar(
    title: String?,
    elevation: Dp = AppBarDefaults.TopAppBarElevation,
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

        },
        actions = actions,
        backgroundColor = CustomTheme.colors.surface,
        elevation = elevation,
    )
}

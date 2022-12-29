package org.zotero.android.uicomponents.topbar

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
fun CustomIconTopBar(
    title: String?,
    iconInt: Int? = null,
    elevation: Dp = AppBarDefaults.TopAppBarElevation,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = {
            if (iconInt != null) {
                Image(
                    painter = painterResource(id = iconInt),
                    contentDescription = null,
                )
            }
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

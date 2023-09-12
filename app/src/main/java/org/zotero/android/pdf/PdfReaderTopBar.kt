package org.zotero.android.pdf

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.Icon
import androidx.compose.material.TopAppBar
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun PdfReaderTopBar(
    onShowHideSideBar: () -> Unit,
    toPdfSettings: () -> Unit,
    elevation: Dp = AppBarDefaults.TopAppBarElevation,
) {
    TopAppBar(
        title = {
            Spacer(modifier = Modifier.width(0.dp))
            Icon(
                modifier = Modifier
                    .size(28.dp)
                    .safeClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple(),
                        onClick = {
                            onShowHideSideBar()
                        },
                    ),
                painter = painterResource(id = Drawables.outline_view_sidebar_24),
                contentDescription = null,
                tint = CustomTheme.colors.zoteroBlueWithDarkMode
            )
        },
        actions = {
            Icon(
                modifier = Modifier
                    .size(28.dp)
                    .safeClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple(),
                        onClick = {
                            toPdfSettings()
                        },
                    ),
                painter = painterResource(id = Drawables.baseline_settings_24),
                contentDescription = null,
                tint = CustomTheme.colors.zoteroBlueWithDarkMode
            )
            Spacer(modifier = Modifier.width(8.dp))
        },
        backgroundColor = CustomTheme.colors.surface,
        elevation = elevation,
    )

}
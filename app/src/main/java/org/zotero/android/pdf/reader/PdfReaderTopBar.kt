package org.zotero.android.pdf.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.Icon
import androidx.compose.material.TopAppBar
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    toggleToolbarButton:() -> Unit,
    isToolbarButtonSelected: Boolean,
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
            ToolbarToggleTopButton(
                isSelected = isToolbarButtonSelected,
                toggleToolbarButton = toggleToolbarButton
            )
            Spacer(modifier = Modifier.width(8.dp))
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

@Composable
internal fun ToolbarToggleTopButton(isSelected: Boolean, toggleToolbarButton: () -> Unit) {
    val tintColor = if (isSelected) {
        Color.White
    } else {
        CustomTheme.colors.zoteroBlueWithDarkMode
    }
    val roundCornerShape = RoundedCornerShape(size = 4.dp)
    var modifier = Modifier
        .size(28.dp)
        .clip(roundCornerShape)
        .safeClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = toggleToolbarButton,
        )
    if (isSelected) {
        modifier = modifier.background(
            color = CustomTheme.colors.zoteroBlueWithDarkMode,
            shape = roundCornerShape
        )
    }
    Icon(
        modifier = modifier,
        painter = painterResource(id = Drawables.outline_edit_24),
        contentDescription = null,
        tint = tintColor
    )
}

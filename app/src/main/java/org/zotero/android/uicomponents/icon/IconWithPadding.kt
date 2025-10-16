package org.zotero.android.uicomponents.icon

import androidx.annotation.DrawableRes
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
fun IconWithPadding(
    modifier: Modifier = Modifier,
    @DrawableRes drawableRes: Int,
    areaSize: Dp = 40.dp,
    iconSize: Dp = 24.dp,
    tintColor: Color? = CustomTheme.colors.zoteroDefaultBlue,
    isEnabled: Boolean = true,
    shouldShowRipple: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .size(areaSize)
            .safeClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = if (shouldShowRipple) {
                    ripple(bounded = false)
                } else {
                    null
                },
                onClick = onClick,
                enabled = isEnabled,
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier.size(iconSize),
            painter = painterResource(id = drawableRes),
            contentDescription = null,
            tint = tintColor ?: LocalContentColor.current,
        )
    }
}
package org.zotero.android.uicomponents.icon

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
            tint = tintColor ?: LocalContentColor.current.copy(alpha = LocalContentAlpha.current),
        )
    }
}

@Composable
fun ToggleIconWithPadding(
    @DrawableRes drawableRes: Int,
    areaSize: Dp = 40.dp,
    iconSize: Dp = 24.dp,
    isSelected: Boolean,
    onToggle: () -> Unit,
) {
    val tintColor = if (isSelected) {
        Color.White
    } else {
        CustomTheme.colors.zoteroDefaultBlue
    }
    val roundCornerShape = RoundedCornerShape(size = 4.dp)
    var modifier = Modifier
        .size(areaSize)
        .clip(roundCornerShape)
        .safeClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onToggle,
        )
    if (isSelected) {
        modifier = modifier.background(
            color = CustomTheme.colors.zoteroDefaultBlue,
            shape = roundCornerShape
        )
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier.size(iconSize),
            painter = painterResource(id = drawableRes),
            contentDescription = null,
            tint = tintColor,
        )
    }
}

@Composable
fun IconWithPadding(
    modifier: Modifier = Modifier,
    areaSize: Dp = 40.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(areaSize)
            .safeClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
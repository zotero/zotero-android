package org.zotero.android.uicomponents.icon

import androidx.annotation.DrawableRes
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource

@Composable
fun IconWithPaddingM3(
    @DrawableRes unselectedDrawableRes: Int,
    @DrawableRes selectedDrawableRes: Int,
    isSelected: Boolean,
    onToggle: (() -> Unit)
) {
    val tintColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val drawableRes = if (isSelected) {
        selectedDrawableRes
    } else {
        unselectedDrawableRes
    }
    IconButton(onClick = onToggle) {
        Icon(
            painter = painterResource(id = drawableRes),
            contentDescription = null,
            tint = tintColor
        )
    }

}

package org.zotero.android.uicomponents.checkbox

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.theme.CustomPalette
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
fun CircleCheckBox(
    isChecked: Boolean,
    modifier: Modifier = Modifier,
    layoutType: CustomLayoutSize.LayoutType,
) {
    Box(
        modifier = modifier
            .size(layoutType.calculateIconSize())
            .clip(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (isChecked) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = CustomTheme.colors.zoteroBlueWithDarkMode,
            )
        } else {
            Icon(
                painter = painterResource(
                    id = Drawables.outline_circle_24
                ),
                contentDescription = null,
                tint = CustomPalette.CoolGray,
            )
        }

    }
}
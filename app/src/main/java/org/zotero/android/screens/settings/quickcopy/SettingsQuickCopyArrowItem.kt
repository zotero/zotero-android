package org.zotero.android.screens.settings.quickcopy

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun SettingsQuickCopyArrowItem(
    title: String,
    text: String,
    isEnabled:Boolean = true,
    onTapped: () -> Unit,
) {
    val disabledColor = Color(0xFF89898C)
    val rightTextAndIconColor = if (isEnabled) {
        CustomTheme.colors.chevronNavigationColor
    } else {
        disabledColor
    }
    val leftTextAndIconColor = if (isEnabled) {
        CustomTheme.colors.primaryContent
    } else {
        disabledColor
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .background(CustomTheme.colors.surface)
            .safeClickable(
                enabled = isEnabled,
                onClick = onTapped,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
            ),
    ) {
        Text(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp, end = 0.dp),
            text = title,
            style = CustomTheme.typography.newBody,
            color = leftTextAndIconColor,
        )
        Text(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 32.dp, start = 140.dp),
            text = text,
            style = CustomTheme.typography.newBody,
            color = rightTextAndIconColor,
        )
        Icon(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp),
            painter = painterResource(id = Drawables.chevron_right_24px),
            contentDescription = null,
            tint = rightTextAndIconColor
        )
    }
}

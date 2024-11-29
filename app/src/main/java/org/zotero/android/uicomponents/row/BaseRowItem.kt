package org.zotero.android.uicomponents.row

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
fun BaseRowItem(
    modifier: Modifier = Modifier,
    title: String,
    description: String? = null,
    titleStyle: TextStyle = CustomTheme.typography.h3,
    textColor: Color = CustomTheme.colors.dynamicTheme.primaryColor,
    backgroundColor: Color = CustomTheme.colors.surface,
    heightIn: Dp = 64.dp,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    startContentPadding: Dp = 0.dp,
    startContent: @Composable (RowScope.() -> Unit)? = null,
    endContent: @Composable (RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .background(backgroundColor)
            .fillMaxWidth()
            .safeClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                enabled = enabled,
                onClick = onClick
            )
            .heightIn(min = heightIn)
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (startContent != null) {
            startContent()
            Spacer(modifier = Modifier.width(startContentPadding))
        }
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                color = textColor,
                text = title,
                style = titleStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (description != null) {
                Text(
                    text = description,
                    modifier = Modifier.padding(top = 4.dp),
                    color = CustomTheme.colors.secondaryContent,
                    style = CustomTheme.typography.h7,
                )
            }
        }
        if (endContent != null) {
            Spacer(modifier = Modifier.width(8.dp))
            endContent()
        }
    }
}

@Composable
fun BaseRowItemWithIcon(
    title: String,
    description: String? = null,
    iconResId: Int? = null,
    iconTint: Color = CustomTheme.colors.dynamicTheme.primaryColor,
    titleStyle: TextStyle = CustomTheme.typography.h3,
    textColor: Color = CustomTheme.colors.zoteroDefaultBlue,
    verticalPadding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    startContentPadding: Dp = 0.dp,
    heightIn: Dp = 64.dp,
    endContent: @Composable (RowScope.() -> Unit)? = null,
) {
    BaseRowItem(
        modifier = Modifier.padding(
            vertical = verticalPadding,
            horizontal = 16.dp
        ),
        title = title,
        textColor = textColor,
        description = description,
        titleStyle = titleStyle,
        heightIn = heightIn,
        onClick = onClick,
        enabled = enabled,
        startContent = {
            if (iconResId != null) {
                /*
                This box ensures that all the texts will have consistent offset
                independently on the size of the icon.
                 */
                Box(
                    modifier = Modifier.width(32.dp)
                ) {
                    Icon(
                        painter = painterResource(iconResId),
                        contentDescription = null,
                        tint = iconTint,
                    )
                }
            }
        },
        startContentPadding = startContentPadding,
        endContent = endContent,
    )
}

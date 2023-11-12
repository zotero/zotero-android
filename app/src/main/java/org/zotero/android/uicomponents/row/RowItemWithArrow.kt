package org.zotero.android.uicomponents.row

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.misc.CustomDivider
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
fun RowItemWithArrow(
    title: String,
    description: String? = null,
    iconResId: Int? = null,
    iconTint: Color = CustomTheme.colors.dynamicTheme.primaryColor,
    titleStyle: TextStyle = CustomTheme.typography.h3,
    startContentPadding: Dp = 0.dp,
    verticalPadding: Dp = 16.dp,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    BaseRowItemWithIcon(
        title = title,
        description = description,
        iconResId = iconResId,
        iconTint = iconTint,
        titleStyle = titleStyle,
        startContentPadding = startContentPadding,
        verticalPadding = verticalPadding,
        onClick = onClick,
        enabled = enabled,
    ) {
        Icon(
            painter = painterResource(Drawables.ic_arrow_small_right),
            contentDescription = null,
            modifier = Modifier.padding(start = 8.dp),
            tint = CustomTheme.colors.zoteroDefaultBlue,
        )
    }
}

@Preview
@Composable
private fun RowItemWithArrowPreview() {
    CustomTheme {
        Column {
            RowItemWithArrow(
                title = "Item with just title",
                onClick = {}
            )
            CustomDivider()
            RowItemWithArrow(
                title = "Item with description",
                description = "This item contains description",
                onClick = {}
            )
            CustomDivider()
            RowItemWithArrow(
                title = "Item with description",
                description = "This item contains description and icon",
                iconResId = Drawables.ic_arrow_small_right,
                onClick = {}
            )
        }
    }
}

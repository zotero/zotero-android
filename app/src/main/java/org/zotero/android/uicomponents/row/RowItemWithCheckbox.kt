package org.zotero.android.uicomponents.row

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.controls.CustomCheckbox
import org.zotero.android.uicomponents.misc.CustomDivider
import org.zotero.android.uicomponents.theme.CustomTheme

/**
 * Row item with an embedded checkbox. Clicking on the whole row toggles the checkbox.
 *
 * Reminder: the state of the checkbox is controlled by [checked] and [enabled]
 * parameters. Don't forget to bind the [onCheckedChange] callback to your state
 * source (preferably in your screen ViewModel). Otherwise clicking on the whole
 * item will do nothing.
 *
 */
@Composable
fun RowItemWithCheckbox(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String? = null,
    iconResId: Int? = null,
    titleStyle: TextStyle = CustomTheme.typography.h3,
    verticalPadding: Dp = 16.dp,
    enabled: Boolean = true,
) {
    BaseRowItemWithIcon(
        title = title,
        description = description,
        iconResId = iconResId,
        titleStyle = titleStyle,
        verticalPadding = verticalPadding,
        onClick = { onCheckedChange(!checked) },
        enabled = enabled,
    ) {
        CustomCheckbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Preview
@Composable
private fun RowItemWithCheckboxPreview() {
    CustomTheme {
        Column {
            RowItemWithCheckbox(
                title = "Item with just title",
                checked = true,
                onCheckedChange = {},
            )
            CustomDivider()
            RowItemWithCheckbox(
                title = "Item with description",
                checked = false,
                onCheckedChange = {},
                description = "This item contains description and may be toggled",
            )
            CustomDivider()
            RowItemWithCheckbox(
                title = "Item with description and icon",
                checked = false,
                onCheckedChange = {},
                description = "This item contains description , tinted icon and may be toggled",
                iconResId = Drawables.ic_lock_solid,
            )
            CustomDivider()
            RowItemWithCheckbox(
                title = "Disabled item",
                checked = true,
                onCheckedChange = {},
                enabled = false,
            )
        }
    }
}

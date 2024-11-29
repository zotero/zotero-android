package org.zotero.android.uicomponents.controls

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.CheckboxColors
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.Icon
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.theme.CustomPalette
import org.zotero.android.uicomponents.theme.CustomTheme

/**
 * Compose component for a checkbox with the style from design system applied.
 */
@Composable
fun CustomCheckbox(
    checked: Boolean,
    modifier: Modifier = Modifier,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    enabled: Boolean = true,
) {

    // If there's a callback we configure the component to make it toggleable
    // via input and accessibility events.
    val toggleableModifier =
        modifier.then(
            if (onCheckedChange != null) {
                Modifier.toggleable(
                    value = checked,
                    enabled = enabled,
                    role = Role.Checkbox,
                    onValueChange = onCheckedChange,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(
                        bounded = false,
                        radius = 24.dp
                    )
                )
            } else {
                modifier
            }
        )

    val colors = CustomCheckbox.colors()
    val state = ToggleableState(checked)
    val checkColor by colors.checkmarkColor(state)
    val boxColor by colors.boxColor(enabled, state)
    val borderColor by colors.borderColor(enabled, state)

    if (checked) {
        Box(
            modifier = toggleableModifier
                .requiredSize(20.dp)
                .clip(CircleShape)
                .background(boxColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = Drawables.check_24px),
                contentDescription = null,
                tint = checkColor
            )
        }
    } else {
        Canvas(toggleableModifier.requiredSize(20.dp)) {
            drawCircle(
                color = borderColor,
                style = Stroke(1.5.dp.toPx())
            )
        }
    }
}

private object CustomCheckbox {

    @Composable
    fun colors(): CheckboxColors {
        val isLight = CustomTheme.colors.isLight

        val defaultBorderColor = if (isLight) {
            CustomPalette.FeatherGray
        } else {
            CustomPalette.LightCharcoal
        }
        val disabledBorderColor = if (isLight) {
            CustomPalette.FogGray
        } else {
            CustomPalette.Charcoal
        }

        return CheckboxDefaults.colors(
            checkedColor = CustomTheme.colors.dynamicTheme.primaryColor,
            uncheckedColor = defaultBorderColor,
            checkmarkColor = CustomTheme.colors.dynamicTheme.buttonTextColor,
            disabledColor = disabledBorderColor,
            disabledIndeterminateColor = CustomPalette.CoolGray
        )
    }
}

@Preview(widthDp = 160, heightDp = 60)
@Composable
fun CustomCheckboxEnabledOnPreview() {
    Column {
        CustomTheme {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(CustomPalette.White),
            ) {
                CustomCheckbox(checked = true, enabled = true)
                CustomCheckbox(checked = false, enabled = true)
                CustomCheckbox(checked = true, enabled = false)
                CustomCheckbox(checked = false, enabled = false)
            }
        }
        CustomTheme(isDarkTheme = true) {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(CustomPalette.Black),
            ) {
                CustomCheckbox(checked = true, enabled = true)
                CustomCheckbox(checked = false, enabled = true)
                CustomCheckbox(checked = true, enabled = false)
                CustomCheckbox(checked = false, enabled = false)
            }
        }
    }
}

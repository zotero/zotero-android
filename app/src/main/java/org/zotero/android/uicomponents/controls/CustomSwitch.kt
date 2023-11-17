package org.zotero.android.uicomponents.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Switch
import androidx.compose.material.SwitchColors
import androidx.compose.material.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.theme.CustomPalette
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
fun CustomSwitch(
    checked: Boolean,
    modifier: Modifier = Modifier,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = CustomSwitch.colors(),
    )
}

private object CustomSwitch {

    @Composable
    fun colors(): SwitchColors {
        val isLight = CustomTheme.colors.isLight
        val disabledThumbColor = if (isLight) {
            CustomPalette.FeatherGray
        } else {
            CustomPalette.Charcoal
        }
        val disabledTrackColor = if (isLight) {
            CustomPalette.LightFogGray
        } else {
            CustomPalette.DarkCharcoal
        }
        return SwitchDefaults.colors(
            checkedThumbColor = CustomTheme.colors.zoteroDefaultBlue,
            checkedTrackColor = CustomTheme.colors.zoteroDefaultBlue,
            checkedTrackAlpha = 1f,
            uncheckedThumbColor = if (isLight) {
                CustomPalette.White
            } else {
                CustomPalette.Charcoal
            },
            uncheckedTrackColor = if (isLight) {
                CustomPalette.FeatherGray
            } else {
                CustomPalette.LightCharcoal
            },
            uncheckedTrackAlpha = 1f,
            disabledCheckedThumbColor = disabledThumbColor,
            disabledCheckedTrackColor = disabledTrackColor,
            disabledUncheckedThumbColor = disabledThumbColor,
            disabledUncheckedTrackColor = disabledTrackColor
        )
    }
}

@Preview
@Composable
fun CustomSwitchPreview() {
    Column {
        CustomTheme(isDarkTheme = false) {
            Row(
                modifier = Modifier
                    .background(CustomTheme.colors.surface)
                    .padding(16.dp)
            ) {
                CustomSwitch(checked = true, enabled = true)
                Spacer(modifier = Modifier.size(8.dp))
                CustomSwitch(checked = false, enabled = true)
                Spacer(modifier = Modifier.size(8.dp))
                CustomSwitch(checked = true, enabled = false)
                Spacer(modifier = Modifier.size(8.dp))
                CustomSwitch(checked = false, enabled = false)
            }
        }
        CustomTheme(isDarkTheme = true) {
            Row(
                modifier = Modifier
                    .background(CustomTheme.colors.surface)
                    .padding(16.dp)
            ) {
                CustomSwitch(checked = true, enabled = true)
                Spacer(modifier = Modifier.size(8.dp))
                CustomSwitch(checked = false, enabled = true)
                Spacer(modifier = Modifier.size(8.dp))
                CustomSwitch(checked = true, enabled = false)
                Spacer(modifier = Modifier.size(8.dp))
                CustomSwitch(checked = false, enabled = false)
            }
        }
    }
}

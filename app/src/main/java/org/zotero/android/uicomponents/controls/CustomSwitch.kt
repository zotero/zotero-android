package org.zotero.android.uicomponents.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
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
    colors: SwitchColors = SwitchDefaults.colors(),
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
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
            checkedTrackColor = CustomTheme.colors.zoteroDefaultBlue.copy(0.5f),
            uncheckedThumbColor = if (isLight) {
                CustomPalette.White
            } else {
                CustomPalette.Charcoal
            },
            uncheckedTrackColor = if (isLight) {
                CustomPalette.FeatherGray
            } else {
                CustomPalette.LightCharcoal
            }.copy(1f),
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

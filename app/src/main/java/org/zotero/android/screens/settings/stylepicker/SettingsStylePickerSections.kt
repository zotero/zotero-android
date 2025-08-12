package org.zotero.android.screens.settings.stylepicker

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.zotero.android.screens.settings.SettingsDivider
import org.zotero.android.screens.settings.SettingsSection
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.theme.CustomTheme


@Composable
internal fun SettingsCiteCitationStylesSection(
    viewState: SettingsStylePickerViewState,
    viewModel: SettingsStylePickerViewModel
) {
    SettingsSection {
        viewState.styles.forEachIndexed { index, style ->
            SettingsCheckmarkItem(
                title = style.title,
                isChecked = style.identifier == viewState.selected,
                onItemTapped = {viewModel.onItemTapped(style)},
            )
            if (index != viewState.styles.size - 1)
                SettingsDivider()
        }
    }
    Spacer(modifier = Modifier.height(30.dp))

}

@Composable
private fun SettingsCheckmarkItem(
    title: String,
    isChecked: Boolean,
    onItemTapped: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .background(CustomTheme.colors.surface)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = { onItemTapped() },
            ),
    ) {
        Text(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(
                    start = 16.dp, end =
                        if (isChecked) {
                            40.dp
                        } else {
                            16.dp
                        }
                ),
            text = title,
            style = CustomTheme.typography.newBody,
            color = CustomTheme.colors.primaryContent,
        )
        if (isChecked) {
            Icon(
                modifier = Modifier
                    .size(32.dp)
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp),
                painter = painterResource(Drawables.check_24px),
                contentDescription = null,
                tint = CustomTheme.colors.zoteroDefaultBlue,
            )
        }

    }
}
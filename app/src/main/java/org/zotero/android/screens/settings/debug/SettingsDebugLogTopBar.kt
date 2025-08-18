package org.zotero.android.screens.settings.debug

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.uicomponents.Plurals
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.quantityStringResource
import org.zotero.android.uicomponents.topbar.NewCustomTopBar
import org.zotero.android.uicomponents.topbar.NewHeadingTextButton

@Composable
internal fun SettingsDebugLogTopBar(
    onBack: () -> Unit,
    numberOfLines: Int,
) {
    NewCustomTopBar(
        title = quantityStringResource(id = Plurals.settings_lines, numberOfLines),
        leftContainerContent = listOf {
            NewHeadingTextButton(
                onClick = onBack,
                text = stringResource(Strings.back_button),
            )
        }
    )
}
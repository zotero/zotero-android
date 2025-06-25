package org.zotero.android.screens.settings.quickcopy

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.topbar.NewCustomTopBar
import org.zotero.android.uicomponents.topbar.NewHeadingTextButton

@Composable
internal fun SettingsQuickCopyTopBar(
    onBack: () -> Unit,
) {
    NewCustomTopBar(
        backgroundColor = CustomTheme.colors.surface,
        title = stringResource(id = Strings.settings_export_title),
        leftContainerContent = listOf {
            NewHeadingTextButton(
                onClick = onBack,
                text = stringResource(Strings.back_button),
            )
        }
    )
}
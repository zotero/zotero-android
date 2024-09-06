package org.zotero.android.screens.libraries

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.icon.IconWithPadding
import org.zotero.android.uicomponents.topbar.NewCustomTopBar

@Composable
internal fun LibrariesTopBar(
    onSettingsTapped: () -> Unit,
) {
    NewCustomTopBar(
        title = stringResource(id = Strings.toolbar_libraries),
        rightContainerContent = listOf {
            IconWithPadding(drawableRes = Drawables.settings_24px, onClick = onSettingsTapped)
        })
}

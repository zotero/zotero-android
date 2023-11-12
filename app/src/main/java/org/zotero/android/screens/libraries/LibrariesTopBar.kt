package org.zotero.android.screens.libraries

import androidx.compose.material.Text
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.icon.IconWithPadding
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun LibrariesTopBar(
    onSettingsTapped: () -> Unit,
    ) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = stringResource(id = Strings.libraries),
                color = CustomTheme.colors.primaryContent,
                style = CustomTheme.typography.h2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {

        },
        actions = {
            IconWithPadding(drawableRes = Drawables.settings_24px, onClick = onSettingsTapped)
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = CustomTheme.colors.surface),
    )
}

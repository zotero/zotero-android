package org.zotero.android.screens.libraries

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings

@Composable
internal fun LibrariesTopBar(
    onSettingsTapped: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(id = Strings.toolbar_libraries),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge
            )
        },
        navigationIcon = {
            //placeholder
            IconButton(onClick = {}) {
            }
        },
        actions = {
            IconButton(onClick = onSettingsTapped) {
                Icon(
                    painter = painterResource(Drawables.settings_24px),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

        },
    )
}

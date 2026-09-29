package org.zotero.android.screens.login

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings

@Composable
internal fun LoginTopBar(
    loginUrl: String?,
    onCancelClicked: () -> Unit,
    onOpenInBrowserClicked: () -> Unit,
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        title = {},
        navigationIcon = {
            IconButton(onClick = onCancelClicked) {
                Icon(
                    painter = painterResource(Drawables.ic_close_24dp),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        actions = {
            if (loginUrl != null) {
                IconButton(onClick = onOpenInBrowserClicked) {
                    Icon(
                        painter = painterResource(Drawables.ic_open_in_browser_24dp),
                        contentDescription = stringResource(Strings.login_open_in_browser),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        },
    )
}

package org.zotero.android.screens.itemdetails.topbars

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import org.zotero.android.uicomponents.Drawables

@Composable
internal fun ItemDetailsTopBar(
    onViewOrEditClicked: () -> Unit,
    onCancelOrBackClicked: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = "",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge
            )
        },
        navigationIcon = {
            IconButton(onClick = onCancelOrBackClicked) {
                Icon(
                    painter = painterResource(Drawables.arrow_back_24dp),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        actions = {
            IconButton(onClick = onViewOrEditClicked) {
                Icon(
                    painter = painterResource(Drawables.edit_24px),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
    )
}

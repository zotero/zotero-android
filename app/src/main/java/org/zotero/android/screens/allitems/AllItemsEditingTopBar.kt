package org.zotero.android.screens.allitems

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings

@Composable
fun AllItemsEditingTopBar(
    selectedKeysSize: Int,
    allSelected: Boolean,
    isCollectionTrash: Boolean,
    onCancelClicked: () -> Unit,
    toggleSelectionState: () -> Unit,
    onEmptyTrash: () -> Unit,
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        title = {
            Text(
                text = stringResource(
                    id = Strings.tag_picker_title,
                    selectedKeysSize
                ),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge
            )
        },
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
            if (isCollectionTrash) {
                TextButton(onClick = onEmptyTrash) {
                    Text(
                        text = stringResource(Strings.collections_empty_trash),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            TextButton(onClick = toggleSelectionState) {
                Text(
                    text = if (allSelected) stringResource(Strings.items_deselect_all) else stringResource(
                        Strings.items_select_all
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    )
}
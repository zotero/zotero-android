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
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.safeStringResource

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
                text = safeStringResource(
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
                        text = safeStringResource(Strings.collections_empty_trash),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            TextButton(onClick = toggleSelectionState) {
                Text(
                    text = if (allSelected) safeStringResource(Strings.items_deselect_all) else safeStringResource(
                        Strings.items_select_all
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    )
}
package org.zotero.android.screens.allitems

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AppBarRow
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.BottomAppBarScrollBehavior
import androidx.compose.material3.FlexibleBottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings

@Composable
internal fun AllItemsSingleItemEditingBottomPanel(
    viewModel: AllItemsViewModel,
    viewState: AllItemsViewState,
    scrollBehavior: BottomAppBarScrollBehavior,
) {
    FlexibleBottomAppBar(
        horizontalArrangement = BottomAppBarDefaults.FlexibleFixedHorizontalArrangement,
        scrollBehavior = scrollBehavior,
        contentPadding = PaddingValues(horizontal = 0.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        content = {
            AppBarRow(
                overflowIndicator = { menuState ->
                    IconButton(
                        onClick = {
                            if (menuState.isExpanded) {
                                menuState.dismiss()
                            } else {
                                menuState.show()
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "Overflow")
                    }
                }
            ) {
                allItemsBottomPanelItem(
                    iconRes = Drawables.create_new_folder_24px,
                    overflowTextResId = Strings.items_action_add_to_collection,
                    onClick = {})
                allItemsBottomPanelItem(
                    iconRes = Drawables.create_new_folder_24px,
                    overflowTextResId = Strings.items_action_add_to_collection,
                    onClick = {})
                allItemsBottomPanelItem(
                    iconRes = Drawables.create_new_folder_24px,
                    overflowTextResId = Strings.items_action_add_to_collection,
                    onClick = {})
                allItemsBottomPanelItem(
                    iconRes = Drawables.create_new_folder_24px,
                    overflowTextResId = Strings.items_action_add_to_collection,
                    onClick = {})
                allItemsBottomPanelItem(
                    iconRes = Drawables.create_new_folder_24px,
                    overflowTextResId = Strings.items_action_add_to_collection,
                    onClick = {})
                allItemsBottomPanelItem(
                    iconRes = Drawables.create_new_folder_24px,
                    overflowTextResId = Strings.items_action_add_to_collection,
                    onClick = {})
                allItemsBottomPanelItem(
                    iconRes = Drawables.create_new_folder_24px,
                    overflowTextResId = Strings.items_action_add_to_collection,
                    onClick = {})
                allItemsBottomPanelItem(
                    iconRes = Drawables.create_new_folder_24px,
                    overflowTextResId = Strings.items_action_add_to_collection,
                    onClick = {})
            }
        })
}

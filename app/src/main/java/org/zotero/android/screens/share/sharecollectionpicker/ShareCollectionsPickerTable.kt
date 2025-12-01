package org.zotero.android.screens.share.sharecollectionpicker

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.zotero.android.screens.share.sharecollectionpicker.rows.ShareCollectionRowItem
import org.zotero.android.screens.share.sharecollectionpicker.rows.shareCollectionRecursiveItem
import org.zotero.android.uicomponents.Drawables

@Composable
internal fun ShareCollectionsPickerTable(
    viewState: ShareCollectionPickerViewState,
    viewModel: ShareCollectionPickerViewModel,
) {
    LazyColumn {
        for (library in viewState.libraries) {
            val listOfCollectionsWithChildren = viewState.treesToDisplay[library.identifier]!!
            val collapsed = viewState.librariesCollapsed[library.identifier] ?: continue
            item {
                ShareCollectionRowItem(
                    levelPadding = 0.dp,
                    iconRes = Drawables.icon_cell_library,
                    title = library.name,
                    hasChildren = listOfCollectionsWithChildren.isNotEmpty(),
                    isSelected = false,
                    isCollapsed = collapsed,
                    forceUpdateKey = viewState.forceUpdateKey,
                    onItemChevronTapped = {
                        viewModel.onLibraryChevronTapped(
                            library.identifier,
                        )
                    },
                    onRowTapped = { viewModel.onItemTapped(library, null) }
                )
            }

            if (!collapsed) {
                shareCollectionRecursiveItem(
                    viewState = viewState,
                    viewModel = viewModel,
                    library = library,
                    collectionItems = listOfCollectionsWithChildren,
                )
            }
        }
        item {
            Spacer(modifier = Modifier.windowInsetsPadding(NavigationBarDefaults.windowInsets))
        }
    }
}
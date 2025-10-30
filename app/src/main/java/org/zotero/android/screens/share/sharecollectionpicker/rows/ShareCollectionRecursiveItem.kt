package org.zotero.android.screens.share.sharecollectionpicker.rows

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.zotero.android.screens.collections.data.CollectionItemWithChildren
import org.zotero.android.screens.share.sharecollectionpicker.ShareCollectionPickerViewModel
import org.zotero.android.screens.share.sharecollectionpicker.ShareCollectionPickerViewState
import org.zotero.android.sync.Library

private val levelPaddingConst = 16.dp

internal fun LazyListScope.shareCollectionRecursiveItem(
    viewState: ShareCollectionPickerViewState,
    viewModel: ShareCollectionPickerViewModel,
    library: Library,
    collectionItems: List<CollectionItemWithChildren>,
    levelPadding: Dp = 4.dp
) {
    for (item in collectionItems) {
        item {
            ShareCollectionRowItem(
                levelPadding = levelPadding,
                iconRes = item.collection.iconName,
                title = item.collection.name,
                hasChildren = item.children.isNotEmpty(),
                forceUpdateKey = viewState.forceUpdateKey,
                isSelected = viewState.selectedCollectionId == item.collection.identifier,
                isCollapsed = viewState.isCollapsed(library.identifier, item),
                onItemChevronTapped = {
                    viewModel.onCollectionChevronTapped(
                        library.identifier,
                        item.collection
                    )
                },
                onRowTapped = { viewModel.onItemTapped(library, item.collection) }
            )
        }

        if (!viewState.isCollapsed(library.identifier, item)) {
            shareCollectionRecursiveItem(
                viewState = viewState,
                viewModel = viewModel,
                library = library,
                collectionItems = item.children,
                levelPadding = levelPadding + levelPaddingConst
            )
        }
    }
}
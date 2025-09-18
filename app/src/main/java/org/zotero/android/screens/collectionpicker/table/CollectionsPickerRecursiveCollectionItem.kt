package org.zotero.android.screens.collectionpicker.table

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.screens.collections.data.CollectionItemWithChildren

private val levelPaddingConst = 16.dp

internal fun LazyListScope.collectionsPickerRecursiveCollectionItem(
    layoutType: CustomLayoutSize.LayoutType,
    levelPadding: Dp = 16.dp,
    collectionItems: ImmutableList<CollectionItemWithChildren>,
    isChecked: (item: CollectionItemWithChildren) -> Boolean,
    onClick: (item: CollectionItemWithChildren) -> Unit
) {
    for (item in collectionItems) {
        item {
            CollectionPickerItem(
                iconName = item.collection.iconName,
                collectionName = item.collection.name,
                levelPadding = levelPadding,
                isSelected = isChecked(item),
                onClick = { onClick(item) },
            )
        }

        collectionsPickerRecursiveCollectionItem(
            layoutType = layoutType,
            levelPadding = levelPadding + levelPaddingConst,
            collectionItems = item.children,
            isChecked = isChecked,
            onClick = onClick,
        )
    }
}

package org.zotero.android.screens.share.sharecollectionpicker

import androidx.annotation.DrawableRes
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.screens.collections.data.CollectionItemWithChildren
import org.zotero.android.sync.Library
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.icon.IconWithPadding
import org.zotero.android.uicomponents.misc.NewDivider
import org.zotero.android.uicomponents.theme.CustomTheme

private val levelPaddingConst = 8.dp

@Composable
internal fun ShareCollectionsPickerTable(
    viewState: ShareCollectionPickerViewState,
    viewModel: ShareCollectionPickerViewModel,
    layoutType: CustomLayoutSize.LayoutType
) {
    LazyColumn(
        state = rememberLazyListState(),
    ) {
        for (library in viewState.libraries) {
            val listOfCollectionsWithChildren = viewState.treesToDisplay[library.identifier]!!
            val collapsed = viewState.librariesCollapsed[library.identifier] ?: continue
            item {
                ShareRowItem(
                    levelPadding = levelPaddingConst,
                    iconRes = Drawables.icon_cell_library,
                    title = library.name,
                    hasChildren = listOfCollectionsWithChildren.isNotEmpty(),
                    isCollapsed = collapsed,
                    onItemChevronTapped = {
                        viewModel.onLibraryChevronTapped(
                            library.identifier,
                        )
                    },
                    onRowTapped = { viewModel.onItemTapped(library, null) }
                )
            }

            if (!collapsed) {
                recursiveCollectionItem(
                    viewState = viewState,
                    viewModel = viewModel,
                    layoutType = layoutType,
                    library = library,
                    collectionItems = listOfCollectionsWithChildren,
                    levelPadding = levelPaddingConst + levelPaddingConst
                )
            }
        }
    }
}


private fun LazyListScope.recursiveCollectionItem(
    viewState: ShareCollectionPickerViewState,
    viewModel: ShareCollectionPickerViewModel,
    layoutType: CustomLayoutSize.LayoutType,
    library: Library,
    collectionItems: List<CollectionItemWithChildren>,
    levelPadding: Dp = 8.dp
) {
    for (item in collectionItems) {
        item {
            ShareRowItem(
                levelPadding = levelPadding,
                iconRes = item.collection.iconName,
                title = item.collection.name,
                hasChildren = item.children.isNotEmpty(),
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
            recursiveCollectionItem(
                viewState = viewState,
                viewModel = viewModel,
                layoutType = layoutType,
                library = library,
                collectionItems = item.children,
                levelPadding = levelPadding + levelPaddingConst
            )
        }
    }
}

@Composable
private fun ShareRowItem(
    levelPadding: Dp,
    @DrawableRes iconRes: Int,
    title: String,
    isCollapsed: Boolean,
    hasChildren: Boolean,
    onItemChevronTapped: () -> Unit,
    onRowTapped: () -> Unit,
) {
    val rowModifier: Modifier = Modifier.height(44.dp)
    val arrowIconAreaSize = 32.dp
    val mainIconSize = 28.dp
    val paddingBetweenIconAndText = 16.dp
    val levelPaddingWithArrowIconAreaSize = levelPadding + arrowIconAreaSize
    val dividerOffset = levelPaddingWithArrowIconAreaSize + mainIconSize + paddingBetweenIconAndText
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = rowModifier
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(),
                    onClick = onRowTapped,
                )
        ) {
            if (!hasChildren) {
                Spacer(modifier = Modifier.width(levelPaddingWithArrowIconAreaSize))
            } else {
                Spacer(modifier = Modifier.width(levelPadding))
                IconWithPadding(
                    drawableRes = if (isCollapsed) {
                        Drawables.chevron_right_24px
                    } else {
                        Drawables.expand_more_24px
                    },
                    onClick = { onItemChevronTapped() },
                    areaSize = arrowIconAreaSize,
                    shouldShowRipple = false
                )
            }
            Icon(
                modifier = Modifier.size(mainIconSize),
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = CustomTheme.colors.zoteroDefaultBlue
            )
            Spacer(modifier = Modifier.width(paddingBetweenIconAndText))

            Text(
                modifier = Modifier.weight(1f),
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = CustomTheme.typography.newBody,
                color = CustomTheme.colors.allItemsRowTitleColor,
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
        NewDivider(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = dividerOffset)
        )
    }
}


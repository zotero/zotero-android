package org.zotero.android.screens.allitems

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.icon.IconWithPadding
import org.zotero.android.uicomponents.misc.NewDivider
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.topbar.NewCustomTopBar
import org.zotero.android.uicomponents.topbar.NewHeadingTextButton

@Composable
internal fun AllItemsTopBar(
    viewState: AllItemsViewState,
    viewModel: AllItemsViewModel,
    layoutType: CustomLayoutSize.LayoutType
) {
    if (layoutType.isTablet()) {
        AllItemsTabletTopBar(viewState, viewModel)
    } else {
        AllItemsPhoneTopBar(viewState, viewModel)
    }
}

@Composable
private fun AllItemsTabletTopBar(
    viewState: AllItemsViewState,
    viewModel: AllItemsViewModel
) {
    Column {
        Row(
            modifier = Modifier
                .height(55.dp)
                .background(color = CustomTheme.colors.topBarBackgroundColor),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.weight(1f))
            NewCustomTopBar(
                leftContainerContent = allItemsTopBarActions(
                    viewState = viewState,
                    viewModel = viewModel
                ),
                shouldFillMaxWidth = false,
                shouldAddBottomDivider = false
            )
            Spacer(modifier = Modifier.width(8.dp))
            AllItemsSearchBar(
                modifier = Modifier
                    .width(250.dp),
                viewState = viewState,
                viewModel = viewModel
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
        NewDivider()
    }

}

@Composable
private fun AllItemsPhoneTopBar(
    viewState: AllItemsViewState,
    viewModel: AllItemsViewModel
) {
    NewCustomTopBar(
        title = if (!viewState.isEditing) {
            viewState.collectionName
        } else {
            null
        },
        leftContainerContent = listOf {
            NewHeadingTextButton(
                onClick = viewModel::navigateToCollections,
                text = stringResource(id = Strings.sync_toolbar_object_collections)
                    .replaceFirstChar(Char::titlecase)
            )
        },
        rightContainerContent = allItemsTopBarActions(viewState, viewModel),
        shouldAddBottomDivider = false,
    )
}

private fun allItemsTopBarActions(
    viewState: AllItemsViewState,
    viewModel: AllItemsViewModel
): List<@Composable (RowScope.() -> Unit)> {
    val buttonsList :MutableList<@Composable (RowScope.() -> Unit)> = mutableListOf()
    if (viewState.isCollectionTrash) {
        buttonsList.add {
            NewHeadingTextButton(
                onClick = viewModel::onEmptyTrash,
                text = stringResource(Strings.collections_empty_trash),
            )
        }
    } else {
        buttonsList.add {
            IconWithPadding(
                drawableRes = Drawables.add_24px,
                onClick = viewModel::onAdd,
                shouldShowRipple = false
            )
        }
    }
    if (!viewState.isEditing) {
        buttonsList.add {
            NewHeadingTextButton(
                onClick = viewModel::onSelect,
                text = stringResource(Strings.select),
            )
        }
    } else {
        val allSelected = viewState.areAllSelected
        if (allSelected) {
            buttonsList.add {
                NewHeadingTextButton(
                    onClick = viewModel::toggleSelectionState,
                    text = stringResource(Strings.items_deselect_all),
                )
            }
        } else {
            buttonsList.add {
                NewHeadingTextButton(
                    onClick = viewModel::toggleSelectionState,
                    text = stringResource(Strings.items_select_all),
                )
            }
        }
        buttonsList.add {
            NewHeadingTextButton(
                onClick = viewModel::onDone,
                text = stringResource(Strings.done),
            )
        }
    }
    return buttonsList
}

//@Composable
//private fun AllItemsTopBarActions(
//    viewState: AllItemsViewState,
//    viewModel: AllItemsViewModel
//) {
//    if (viewState.collection.identifier.isTrash) {
//        NewHeadingTextButton(
//            onClick = viewModel::onEmptyTrash,
//            text = stringResource(Strings.collections_empty_trash),
//        )
//        Spacer(modifier = Modifier.width(16.dp))
//    } else {
//        IconWithPadding(
//            drawableRes = Drawables.add_24px,
//            onClick = viewModel::onAdd,
//            shouldShowRipple = false
//        )
//        Spacer(modifier = Modifier.width(8.dp))
//    }
//
//    if (!viewState.isEditing) {
//        NewHeadingTextButton(
//            onClick = viewModel::onSelect,
//            text = stringResource(Strings.select),
//        )
////        Spacer(modifier = Modifier.width(8.dp))
//    } else {
//        val allSelected = viewState.selectedItems.size == (viewModel.results?.size ?: 0)
//        if (allSelected) {
//            NewHeadingTextButton(
//                onClick = viewModel::toggleSelectionState,
//                text = stringResource(Strings.items_deselect_all),
//            )
//        } else {
//            NewHeadingTextButton(
//                onClick = viewModel::toggleSelectionState,
//                text = stringResource(Strings.items_select_all),
//            )
//        }
//        Spacer(modifier = Modifier.width(16.dp))
//
//        NewHeadingTextButton(
//            onClick = viewModel::onDone,
//            text = stringResource(Strings.done),
//        )
////        Spacer(modifier = Modifier.width(4.dp))
//    }
//}

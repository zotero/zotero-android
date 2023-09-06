package org.zotero.android.screens.allitems

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.topbar.HeadingTextButton

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
    Row(verticalAlignment = Alignment.CenterVertically) {
        AllItemsSearchBar(
            modifier = Modifier
                .padding(top = 12.dp)
                .weight(1f),
            viewState = viewState,
            viewModel = viewModel
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            AllItemsTopBarActions(
                viewState = viewState,
                viewModel = viewModel
            )
        }
    }
}

@Composable
private fun AllItemsPhoneTopBar(
    viewState: AllItemsViewState,
    viewModel: AllItemsViewModel
) {
    CenterAlignedTopAppBar(
        title = {
            if (!viewState.isEditing) {
                Text(
                    text = viewState.collection.name,
                    color = CustomTheme.colors.primaryContent,
                    style = CustomTheme.typography.h2
                )
            }
        },
        navigationIcon = {
            Row {
                Spacer(modifier = Modifier.width(8.dp))
                HeadingTextButton(
                    isEnabled = true,
                    onClick = viewModel::navigateToCollections,
                    text = stringResource(id = Strings.collections)
                )
            }
        },
        actions = {
            AllItemsTopBarActions(viewState, viewModel)

        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = CustomTheme.colors.surface),
    )
}

@Composable
private fun AllItemsTopBarActions(
    viewState: AllItemsViewState,
    viewModel: AllItemsViewModel
) {
    if (viewState.collection.identifier.isTrash) {
        HeadingTextButton(
            onClick = viewModel::onEmptyTrash,
            text = stringResource(Strings.collections_empty_trash),
        )
    } else {
        Icon(
            modifier = Modifier.safeClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = false),
                onClick = viewModel::onAdd
            ),
            painter = painterResource(id = Drawables.baseline_add_24),
            contentDescription = null,
            tint = CustomTheme.colors.dynamicTheme.primaryColor,
        )
    }

    Spacer(modifier = Modifier.width(8.dp))

    if (!viewState.isEditing) {
        HeadingTextButton(
            onClick = viewModel::onSelect,
            text = stringResource(Strings.select),
        )
        Spacer(modifier = Modifier.width(8.dp))
    } else {
        val allSelected = viewState.selectedItems.size == (viewModel.results?.size ?: 0)
        if (allSelected) {
            HeadingTextButton(
                onClick = viewModel::toggleSelectionState,
                text = stringResource(Strings.items_deselect_all),
            )
        } else {
            HeadingTextButton(
                onClick = viewModel::toggleSelectionState,
                text = stringResource(Strings.items_select_all),
            )
        }
        Spacer(modifier = Modifier.width(8.dp))

        HeadingTextButton(
            onClick = viewModel::onDone,
            text = stringResource(Strings.done),
        )
        Spacer(modifier = Modifier.width(8.dp))
    }
}

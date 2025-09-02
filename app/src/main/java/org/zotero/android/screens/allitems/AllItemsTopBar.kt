package org.zotero.android.screens.allitems

import androidx.compose.runtime.Composable
import org.zotero.android.architecture.ui.CustomLayoutSize

@Composable
internal fun AllItemsTopBar(
    viewState: AllItemsViewState,
    viewModel: AllItemsViewModel,
    layoutType: CustomLayoutSize.LayoutType,
) {
    if (layoutType.isTablet()) {
        AllItemsTabletSearchBar(viewState, viewModel)
    } else {
        AllItemsPhoneAppSearchBar(viewState, viewModel)
    }
}

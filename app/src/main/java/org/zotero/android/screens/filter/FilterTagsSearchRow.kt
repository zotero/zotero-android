package org.zotero.android.screens.filter

import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun FilterTagsSearchRow(
    viewState: FilterViewState,
    viewModel: FilterViewModel,
    layoutType: CustomLayoutSize.LayoutType
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        FilterTagsSearchBar(
            viewState = viewState,
            viewModel = viewModel
        )
        Box {
            if (viewState.showFilterOptionsPopup) {
                FilterOptionsPopup(
                    viewState = viewState,
                    viewModel = viewModel,
                )
            }
            Image(
                modifier = Modifier
                    .size(layoutType.calculateIconSize())
                    .safeClickable(
                        onClick = viewModel::onMoreSearchOptionsClicked,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false)
                    ),
                painter = painterResource(id = Drawables.more_horiz_24px),
                contentDescription = null,
                colorFilter = ColorFilter.tint(CustomTheme.colors.zoteroDefaultBlue),
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
    }
}

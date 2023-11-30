package org.zotero.android.screens.filter

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun FilterScreen(
    viewModel: FilterViewModel,
    viewState: FilterViewState,
) {
    val layoutType = CustomLayoutSize.calculateLayoutType()
    val isTablet = layoutType.isTablet()

    Column {
        if (!isTablet) {
            DownloadFilesPart(viewState, viewModel)
        }
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterTagsSearchBar(
                        viewState = viewState,
                        viewModel = viewModel
                    )
                    Image(
                        modifier = Modifier
                            .size(layoutType.calculateIconSize())
                            .safeClickable(
                                onClick = viewModel::onMoreSearchOptionsClicked,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = rememberRipple(bounded = false)
                            ),
                        painter = painterResource(id = Drawables.more_horiz_24px),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(CustomTheme.colors.zoteroDefaultBlue),
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                }
            }
            items(items = viewState.tags) { chunkedList ->
                FlowRow(
                    modifier = Modifier,
                ) {
                    chunkedList.forEach {
                        var rowModifier: Modifier = Modifier
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                            .clip(shape = RoundedCornerShape(16.dp))
                        val selected = viewState.selectedTags.contains(it.tag.name)
                        if (selected) {
                            rowModifier = rowModifier
                                .background(CustomTheme.colors.zoteroBlueWithDarkMode.copy(alpha = 0.25f))
                                .border(
                                    width = 1.dp,
                                    color = CustomTheme.colors.zoteroBlueWithDarkMode,
                                    shape = RoundedCornerShape(16.dp)
                                )
                        }
                        Box(
                            modifier = rowModifier
                                .safeClickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { viewModel.onTagTapped(it) },
                                )
                        ) {
                            val textColor = if (it.tag.color.isNotEmpty()) {
                                val color = android.graphics.Color.parseColor(it.tag.color)
                                Color(color)
                            } else {
                                CustomTheme.colors.primaryContent
                            }
                            Text(
                                modifier = Modifier.padding(
                                    vertical = 4.dp,
                                    horizontal = 14.dp
                                ),
                                text = it.tag.name,
                                color = if (it.isActive) textColor else textColor.copy(alpha = 0.55f),
                                style = CustomTheme.typography.newBody,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

    }
    val dialog = viewState.dialog
    if (dialog != null) {
        FilterDeleteAutomaticTagsDialog(
            filterDialog = dialog,
            onDismissDialog = viewModel::onDismissDialog,
            onDeleteAutomaticTags = { viewModel.deleteAutomaticTags() }
        )
    }

    if (viewState.showFilterOptionsPopup) {
        FilterOptionsPopup(
            viewState = viewState,
            viewModel = viewModel,
        )
    }
}
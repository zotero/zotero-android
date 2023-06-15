package org.zotero.android.screens.libraries

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Divider
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.screens.libraries.data.LibraryRowData
import org.zotero.android.screens.libraries.data.LibraryState
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun LibrariesTable(
    viewState: LibrariesViewState,
    viewModel: LibrariesViewModel,
    layoutType: CustomLayoutSize.LayoutType
) {
    LazyColumn(
        state = rememberLazyListState(),
    ) {
        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
        itemsIndexed(viewState.customLibraries) { index, item ->
            LibrariesItem(
                item = item,
                layoutType = layoutType,
                isLastItem = index == viewState.customLibraries.size - 1,
                onItemTapped = { viewModel.onCustomLibraryTapped(index) }
            )
        }

        if (!viewState.groupLibraries.isEmpty()) {
            item {
                Spacer(modifier = Modifier.height(26.dp))
                Text(
                    modifier = Modifier.padding(start = 54.dp, bottom = 4.dp),
                    text = stringResource(id = Strings.group_libraries),
                    fontSize = layoutType.calculateLibraryRowTextSize(),
                    color = CustomTheme.colors.secondaryContent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            itemsIndexed(viewState.groupLibraries) { index, item ->
                LibrariesItem(
                    item = item,
                    layoutType = layoutType,
                    isLastItem = index == viewState.groupLibraries.size - 1,
                    onItemTapped = { viewModel.onGroupLibraryTapped(index) }
                )
            }
        }
    }
}

@Composable
private fun LibrariesItem(
    item: LibraryRowData,
    layoutType: CustomLayoutSize.LayoutType,
    isLastItem: Boolean,
    onItemTapped: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(CustomTheme.colors.surface)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(),
                onClick = { onItemTapped() },
            )
    ) {
        Spacer(modifier = Modifier.width(16.dp))
        Icon(
            modifier = Modifier.size(layoutType.calculateItemsRowMainIconSize()),
            painter = painterResource(id = image(item.state)),
            contentDescription = null,
            tint = CustomTheme.colors.zoteroBlueWithDarkMode
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.name,
                        fontSize = 14.sp,
                        maxLines = 1,
                        style = CustomTheme.typography.h4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (!isLastItem) {
                TableDivider()
            }
        }
    }
}

private fun image(state: LibraryState): Int {
    when (state) {
        LibraryState.normal -> return Drawables.icon_cell_library
        LibraryState.locked -> return Drawables.icon_cell_library_readonly
        LibraryState.archived -> return Drawables.library_archived
    }
}

@Composable
private fun TableDivider(modifier: Modifier = Modifier) {
    Divider(
        modifier = modifier,
        color = CustomTheme.colors.libraryDividerBackground,
        thickness = 1.dp
    )

}
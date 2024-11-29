package org.zotero.android.screens.libraries

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Divider
import androidx.compose.material.ripple
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
import org.zotero.android.screens.libraries.data.LibraryRowData
import org.zotero.android.screens.libraries.data.LibraryState
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.misc.NewDivider
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun LibrariesTable(
    viewState: LibrariesViewState,
    viewModel: LibrariesViewModel,
) {
    LazyColumn(
        state = rememberLazyListState(),
    ) {
        item {
            Spacer(modifier = Modifier.height(30.dp))
            NewDivider()
        }
        itemsIndexed(viewState.customLibraries) { index, item ->
            LibrariesItem(
                item = item,
                isLastItem = index == viewState.customLibraries.size - 1,
                onItemTapped = { viewModel.onCustomLibraryTapped(index) },
                onItemLongTapped = {
                    //no-op
                }
            )
        }
        item {
            NewDivider()
        }

        if (viewState.groupLibraries.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(26.dp))
                Text(
                    modifier = Modifier.padding(start = 60.dp, bottom = 8.dp),
                    text = stringResource(id = Strings.libraries_group_libraries).uppercase(),
                    color = CustomTheme.colors.secondaryContent,
                    maxLines = 1,
                    style = CustomTheme.typography.info,
                    overflow = TextOverflow.Ellipsis
                )
            }
            item {
                NewDivider()
            }
            itemsIndexed(viewState.groupLibraries) { index, item ->
                Box {
                    if (viewState.groupIdForDeletePopup == item.id) {
                        DeleteGroupPopup(
                            onDeleteGroup = { viewModel.showDeleteGroupQuestion(item.id, item.name) },
                            dismissDeleteGroupPopup = { viewModel.dismissDeleteGroupPopup() },
                        )
                    }
                    LibrariesItem(
                        item = item,
                        isLastItem = index == viewState.groupLibraries.size - 1,
                        onItemTapped = { viewModel.onGroupLibraryTapped(index) },
                        onItemLongTapped = { viewModel.showDeleteGroupPopup(item) }
                    )
                }

            }
            item {
                NewDivider()
            }
        }
    }
}

@Composable
private fun LibrariesItem(
    item: LibraryRowData,
    isLastItem: Boolean,
    onItemTapped: () -> Unit,
    onItemLongTapped: () -> Unit,
) {
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
//                .fillMaxWidth()
                .height(44.dp)
                .background(CustomTheme.colors.surface)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(),
                    onClick = onItemTapped,
                    onLongClick = onItemLongTapped,
                )
        ) {
            Spacer(modifier = Modifier.width(16.dp))
            Icon(
                modifier = Modifier.size(28.dp),
                painter = painterResource(id = image(item.state)),
                contentDescription = null,
                tint = CustomTheme.colors.zoteroDefaultBlue
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                modifier = Modifier.weight(1f),
                color = CustomTheme.colors.primaryContent,
                text = item.name,
                maxLines = 1,
                style = CustomTheme.typography.newBody,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
        if (!isLastItem) {
            TableDivider(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 60.dp)
            )
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
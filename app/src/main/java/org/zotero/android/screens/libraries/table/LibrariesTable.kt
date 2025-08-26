package org.zotero.android.screens.libraries.table

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import org.zotero.android.screens.libraries.DeleteGroupPopup
import org.zotero.android.screens.libraries.LibrariesViewModel
import org.zotero.android.screens.libraries.LibrariesViewState
import org.zotero.android.screens.settings.elements.NewSettingsDivider
import org.zotero.android.uicomponents.Strings

@Composable
internal fun LibrariesTable(
    viewState: LibrariesViewState,
    viewModel: LibrariesViewModel,
) {
    LazyColumn(
        state = rememberLazyListState(),
    ) {
        itemsIndexed(viewState.customLibraries) { index, item ->
            LibrariesItem(
                item = item,
                onItemTapped = { viewModel.onCustomLibraryTapped(index) },
                onItemLongTapped = {
                    //no-op
                }
            )
        }

        if (viewState.groupLibraries.isNotEmpty()) {
            item {
                NewSettingsDivider()
                LibrariesSectionTitle(Strings.libraries_group_libraries)
            }
            itemsIndexed(viewState.groupLibraries) { index, item ->
                Box {
                    if (viewState.groupIdForDeletePopup == item.id) {
                        DeleteGroupPopup(
                            onDeleteGroup = {
                                viewModel.showDeleteGroupQuestion(
                                    item.id,
                                    item.name
                                )
                            },
                            dismissDeleteGroupPopup = { viewModel.dismissDeleteGroupPopup() },
                        )
                    }
                    LibrariesItem(
                        item = item,
                        onItemTapped = { viewModel.onGroupLibraryTapped(index) },
                        onItemLongTapped = { viewModel.showDeleteGroupPopup(item) }
                    )
                }
            }
        }
    }
}


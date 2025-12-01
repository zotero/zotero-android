package org.zotero.android.screens.tagpicker

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.screens.tagpicker.row.TagPickerCreateTagRow
import org.zotero.android.screens.tagpicker.row.TagPickerRow
import org.zotero.android.uicomponents.CustomScaffoldM3
import org.zotero.android.uicomponents.themem3.AppThemeM3

@Composable
internal fun TagPickerScreen(
    onBack: () -> Unit,
    viewModel: TagPickerViewModel = hiltViewModel(),
) {
    AppThemeM3 {
        val viewState by viewModel.viewStates.observeAsState(TagPickerViewState())
        val viewEffect by viewModel.viewEffects.observeAsState()
        LaunchedEffect(key1 = viewModel) {
            viewModel.init()
        }

        LaunchedEffect(key1 = viewEffect) {
            when (viewEffect?.consume()) {
                null -> Unit
                is TagPickerViewEffect.OnBack -> {
                    onBack()
                }
            }
        }
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
        CustomScaffoldM3(
            scrollBehavior = scrollBehavior,
            topBar = {
                TagPickerTopBar(
                    onCancelClicked = onBack,
                    onSave = viewModel::onSave,
                    viewState = viewState,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) {
            Column {
                val searchValue = viewState.searchTerm
                var searchBarTextFieldState by remember {
                    mutableStateOf(
                        TextFieldValue(searchValue)
                    )
                }
                val searchBarOnInnerValueChanged: (TextFieldValue) -> Unit = {
                    searchBarTextFieldState = it
                    viewModel.search(it.text)
                }
                val onSearchAction = {
                    viewModel.addTagIfNeeded()
                    searchBarOnInnerValueChanged.invoke(TextFieldValue())
                }

                TagPickerSearchBar(
                    onSearchAction = onSearchAction,
                    searchBarOnInnerValueChanged = searchBarOnInnerValueChanged,
                    searchBarTextFieldState = searchBarTextFieldState
                )

                LazyColumn {
                    items(items = viewState.tags) { tag ->
                        val isChecked = viewState.selectedTags.contains(tag.name)
                        TagPickerRow(
                            text = tag.name,
                            tagColorHex = tag.color,
                            isChecked = isChecked,
                            onTap = { viewModel.selectOrDeselect(tag.name) })
                    }
                    if (viewState.showAddTagButton) {
                        item {
                            TagPickerCreateTagRow(
                                tagName = viewState.searchTerm,
                                onTap = onSearchAction,
                            )
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.windowInsetsPadding(NavigationBarDefaults.windowInsets))
                    }
                }

            }
        }
    }
}
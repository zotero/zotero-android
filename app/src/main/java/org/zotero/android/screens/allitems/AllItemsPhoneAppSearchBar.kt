package org.zotero.android.screens.allitems

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults.rememberTooltipPositionProvider
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.textinput.AppSearchBarM3

@Composable
internal fun AllItemsPhoneAppSearchBar(
    viewState: AllItemsViewState,
    viewModel: AllItemsViewModel,
) {
    var isInSearchMode by remember { mutableStateOf(false) }

    if (!viewState.isEditing) {
        Crossfade(
            modifier = Modifier.animateContentSize(),
            targetState = isInSearchMode,
        ) { searchModeActive ->
            if (!searchModeActive) {
                TopAppBar(
                    title = {
                        Text(
                            text = viewState.collectionName,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = viewModel::navigateToCollections) {
                            Icon(
                                painter = painterResource(Drawables.arrow_back_24dp),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    },
                    actions = {
                        TooltipBox(
                            positionProvider = rememberTooltipPositionProvider(
                                TooltipAnchorPosition.Below,
                                4.dp
                            ),
                            tooltip = {
                                PlainTooltip() {
                                    Text(
                                        stringResource(
                                            Strings.searchbar_placeholder
                                        )
                                    )
                                }
                            },
                            state = rememberTooltipState()
                        ) {
                            IconButton(onClick = { isInSearchMode = !isInSearchMode }) {
                                Icon(
                                    painter = painterResource(Drawables.search_24px),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                        }

                    },
                )
            } else {
                AppSearchBarM3Wrapper(
                    text = viewState.searchTerm ?: "",
                    onValueChanged = { viewModel.onSearch(it) },
                    onBack = {
                        viewModel.onSearch("")
                        isInSearchMode = !isInSearchMode
                    },
                )
            }
        }

    } else {

    }
}

@Composable
private fun AppSearchBarM3Wrapper(
    text: String?,
    onValueChanged: (String) -> Unit,
    onBack: () -> Unit,
) {
    val searchValue = text
    var searchBarTextFieldState by remember {
        mutableStateOf(
            TextFieldValue(
                searchValue ?: ""
            )
        )
    }
    val searchBarOnInnerValueChanged: (TextFieldValue) -> Unit = {
        searchBarTextFieldState = it
        onValueChanged(it.text)
    }
    val onSearchAction = {
        //no-op
    }

    AppSearchBarM3(
        hint = stringResource(id = Strings.items_search_title),
        onSearchImeClicked = onSearchAction,
        onInnerValueChanged = searchBarOnInnerValueChanged,
        textFieldState = searchBarTextFieldState,
        onBack = onBack,
    )
}

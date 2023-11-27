package org.zotero.android.screens.filter

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.screens.filter.data.FilterDialog
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Plurals
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.bottomsheet.LongPressBottomSheet
import org.zotero.android.uicomponents.controls.CustomSwitch
import org.zotero.android.uicomponents.foundation.quantityStringResource
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.misc.CustomDivider
import org.zotero.android.uicomponents.modal.CustomAlertDialog
import org.zotero.android.uicomponents.textinput.SearchBar
import org.zotero.android.uicomponents.theme.CustomPalette
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.theme.CustomThemeWithStatusAndNavBars

@Composable
internal fun FilterScreen(
    onBack: () -> Unit,
    viewModel: FilterViewModel = hiltViewModel(),
) {
    val backgroundColor = CustomTheme.colors.popupBackgroundContent
    CustomThemeWithStatusAndNavBars(
        statusBarBackgroundColor = CustomTheme.colors.topBarBackgroundColor,
        navBarBackgroundColor = backgroundColor
    ) {
        val layoutType = CustomLayoutSize.calculateLayoutType()
        val viewState by viewModel.viewStates.observeAsState(FilterViewState())
        val viewEffect by viewModel.viewEffects.observeAsState()
        LaunchedEffect(key1 = viewModel) {
            viewModel.init()
        }

        LaunchedEffect(key1 = viewEffect) {
            when (viewEffect?.consume()) {
                is FilterViewEffect.OnBack -> {
                    onBack()
                }
                else -> {}
            }
        }
        CustomScaffold(
            backgroundColor = backgroundColor,
            topBar = {
                FilterTopBar(
                    onDone = viewModel::onDone,
                )
            },
        ) {

            Column {
                Row(
                    modifier = Modifier.padding(top = 4.dp, start = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = stringResource(id = Strings.items_filters_downloads),
                        maxLines = 1,
                        style = CustomTheme.typography.newBody,
                    )
                    CustomSwitch(
                        checked = viewState.isDownloadsChecked,
                        onCheckedChange = { viewModel.onDownloadsTapped() },
                        modifier = Modifier
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                CustomDivider()
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TagsSearchBar(
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
                ShowFilterDialog(
                    filterDialog = dialog,
                    onDismissDialog = viewModel::onDismissDialog,
                    onDeleteAutomaticTags = { viewModel.deleteAutomaticTags() }
                )
            }

            LongPressBottomSheet(
                layoutType = layoutType,
                longPressOptionsHolder = viewState.longPressOptionsHolder,
                onCollapse = viewModel::dismissBottomSheet,
                onOptionClick = viewModel::onLongPressOptionsItemSelected,
                tabletWidthPercentage = 0.6f,
            )
        }
    }
}



@Composable
private fun RowScope.TagsSearchBar(
    viewState: FilterViewState,
    viewModel: FilterViewModel
) {
    val searchValue = viewState.searchTerm
    var searchBarTextFieldState by remember {
        mutableStateOf(
            TextFieldValue(searchValue)
        )
    }
    val searchBarOnInnerValueChanged: (TextFieldValue) -> Unit = {
        searchBarTextFieldState = it
        viewModel.onSearch(it.text)
    }
    val onSearchAction = {
        searchBarOnInnerValueChanged.invoke(TextFieldValue())
    }

    SearchBar(
        hint = stringResource(id = Strings.searchbar_placeholder),
        modifier = Modifier
            .weight(1f)
            .padding(start = 16.dp, end = 12.dp),
        onSearchImeClicked = onSearchAction,
        onInnerValueChanged = searchBarOnInnerValueChanged,
        textFieldState = searchBarTextFieldState,
    )
}

@Composable
internal fun ShowFilterDialog(
    filterDialog: FilterDialog,
    onDismissDialog: () -> Unit,
    onDeleteAutomaticTags: () -> Unit,
) {
    when (filterDialog) {
        is FilterDialog.confirmDeletion -> {
            CustomAlertDialog(
                title = stringResource(id = Strings.tag_picker_confirm_deletion_question),
                description = quantityStringResource(
                    id = Plurals.tag_picker_confirm_deletion,
                    filterDialog.count
                ),
                primaryAction = CustomAlertDialog.ActionConfig(
                    text = stringResource(id = Strings.cancel),
                ),
                secondaryAction = CustomAlertDialog.ActionConfig(
                    text = stringResource(id = Strings.ok),
                    textColor = CustomPalette.ErrorRed,
                    onClick = {
                        onDeleteAutomaticTags()
                    }
                ),
                onDismiss = onDismissDialog
            )
        }

        else -> {}
    }
}


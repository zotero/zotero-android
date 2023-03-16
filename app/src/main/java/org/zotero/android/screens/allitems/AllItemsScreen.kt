
package org.zotero.android.screens.allitems

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Icon
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
import androidx.compose.ui.Alignment.Companion.CenterEnd
import androidx.compose.ui.Alignment.Companion.CenterStart
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Alignment.Companion.TopStart
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.database.objects.RItem
import org.zotero.android.screens.allitems.data.ItemCellModel
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.attachmentprogress.FileAttachmentView
import org.zotero.android.uicomponents.attachmentprogress.Style
import org.zotero.android.uicomponents.bottomsheet.LongPressBottomSheet
import org.zotero.android.uicomponents.error.FullScreenError
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.loading.BaseLceBox
import org.zotero.android.uicomponents.loading.CircularLoading
import org.zotero.android.uicomponents.loading.IconLoadingPlaceholder
import org.zotero.android.uicomponents.loading.TextLoadingPlaceholder
import org.zotero.android.uicomponents.misc.CustomDivider
import org.zotero.android.uicomponents.modal.CustomModalBottomSheet
import org.zotero.android.uicomponents.row.RowItemWithArrow
import org.zotero.android.uicomponents.textinput.SearchBar
import org.zotero.android.uicomponents.theme.CustomPalette
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.topbar.CloseIconTopBar
import org.zotero.android.uicomponents.topbar.HeadingTextButton
import java.io.File

@Composable
@Suppress("UNUSED_PARAMETER")
internal fun AllItemsScreen(
    onBack: () -> Unit,
    viewModel: AllItemsViewModel = hiltViewModel(),
    onPickFile: () -> Unit,
    onOpenFile: (file: File, mimeType: String) -> Unit,
    onOpenWebpage: (uri: Uri) -> Unit,
    navigateToSinglePickerScreen: () -> Unit,
    navigateToSinglePickerDialog: () -> Unit,
    navigateToAllItemsSortScreen: () -> Unit,
    navigateToAllItemsSortDialog: () -> Unit,
    navigateToItemDetails: () -> Unit,
    navigateToAddOrEditNote: () -> Unit,
    navigateToVideoPlayerScreen: () -> Unit,
    navigateToImageViewerScreen: () -> Unit,
    navigateToFilterScreen: () -> Unit,
    navigateToFilterDialog: () -> Unit,
    onShowPdf: (file: File) -> Unit,
) {
    val layoutType = CustomLayoutSize.calculateLayoutType()
    val viewState by viewModel.viewStates.observeAsState(AllItemsViewState())
    val viewEffect by viewModel.viewEffects.observeAsState()
    LaunchedEffect(key1 = viewModel) {
        viewModel.init()
    }

    LaunchedEffect(key1 = viewEffect) {
        when (val consumedEffect = viewEffect?.consume()) {
            null -> Unit
            is AllItemsViewEffect.ShowItemDetailEffect -> navigateToItemDetails()
            is AllItemsViewEffect.ShowAddOrEditNoteEffect -> navigateToAddOrEditNote()
            AllItemsViewEffect.ShowFilterEffect -> {
                when (layoutType.showScreenOrDialog()) {
                    CustomLayoutSize.ScreenOrDialogToShow.SCREEN -> {
                        navigateToFilterScreen()
                    }
                    CustomLayoutSize.ScreenOrDialogToShow.DIALOG -> {
                        navigateToFilterDialog()
                    }
                }
            }
            AllItemsViewEffect.ShowItemTypePickerEffect -> {
                when (layoutType.showScreenOrDialog()) {
                    CustomLayoutSize.ScreenOrDialogToShow.SCREEN -> {
                        navigateToSinglePickerScreen()
                    }
                    CustomLayoutSize.ScreenOrDialogToShow.DIALOG -> {
                        navigateToSinglePickerDialog()
                    }
                }
            }
            AllItemsViewEffect.ShowSortPickerEffect -> {
                when (layoutType.showScreenOrDialog()) {
                    CustomLayoutSize.ScreenOrDialogToShow.SCREEN -> {
                        navigateToAllItemsSortScreen()
                    }
                    CustomLayoutSize.ScreenOrDialogToShow.DIALOG -> {
                        navigateToAllItemsSortDialog()
                    }
                }
            }
            AllItemsViewEffect.ScreenRefresh -> {
                //no-op
            }
            is AllItemsViewEffect.OpenFile -> onOpenFile(
                consumedEffect.file,
                consumedEffect.mimeType
            )
            is AllItemsViewEffect.OpenWebpage -> onOpenWebpage(consumedEffect.uri)
            is AllItemsViewEffect.ShowPdf -> {
                onShowPdf(consumedEffect.file)
            }
            is AllItemsViewEffect.ShowVideoPlayer -> {
                navigateToVideoPlayerScreen()
            }
            is AllItemsViewEffect.ShowImageViewer -> {
                navigateToImageViewerScreen()
            }
        }
    }

    CustomScaffold(
        topBar = {
            TopBar(
                onCloseClicked = onBack,
                onAddClicked = viewModel::onAdd,
                title = viewState.collection.name
            )
        },
    ) {
        BaseLceBox(
            modifier = Modifier.fillMaxSize(),
            lce = viewState.lce,
            error = { lceError ->
                FullScreenError(
                    modifier = Modifier.align(Alignment.Center),
                    errorTitle = stringResource(id = Strings.all_items_load_error),
                )
            },
            loading = {
                CircularLoading()
            },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(layoutType.calculateAllItemsBottomPanelHeight())
                    .align(Alignment.BottomStart)
            ) {
                CustomDivider(modifier = Modifier.align(TopStart))
                Icon(
                    modifier = Modifier
                        .padding(end = 30.dp)
                        .size(layoutType.calculateItemsRowInfoIconSize())
                        .align(CenterEnd)
                        .safeClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = rememberRipple(),
                            onClick = {
                                viewModel.showSortPicker()
                            }
                        ),
                    painter = painterResource(id = Drawables.baseline_swap_vert_24),
                    contentDescription = null,
                    tint = CustomTheme.colors.zoteroBlueWithDarkMode
                )
                Icon(
                    modifier = Modifier
                        .padding(start = 30.dp)
                        .size(layoutType.calculateItemsRowInfoIconSize())
                        .align(CenterStart)
                        .safeClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = rememberRipple(),
                            onClick = {
                                viewModel.showFilters()
                            }
                        ),
                    painter = painterResource(id = Drawables.baseline_filter_list_24),
                    contentDescription = null,
                    tint = CustomTheme.colors.zoteroBlueWithDarkMode
                )
            }

            Column(modifier = Modifier.padding(bottom = layoutType.calculateAllItemsBottomPanelHeight())) {
                val searchValue = viewState.searchTerm
                var searchBarTextFieldState by remember {
                    mutableStateOf(
                        TextFieldValue(
                            searchValue ?: ""
                        )
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
                    hint = stringResource(id = Strings.search_items),
                    modifier = Modifier.padding(12.dp),
                    onSearchImeClicked = onSearchAction,
                    onInnerValueChanged = searchBarOnInnerValueChanged,
                    textFieldState = searchBarTextFieldState,
                )
                CustomDivider()
                LazyColumn(
                    state = rememberLazyListState(),
                ) {
                    itemsIndexed(
                        items = viewState.snapshot!!
                    ) { index, item ->
                        Box(modifier = Modifier.animateItemPlacement()) {
                            ItemView(
                                rItem = item,
                                layoutType = layoutType,
                                viewState = viewState,
                                viewModel = viewModel,
                                showBottomDivider = index != viewState.snapshot!!.size - 1
                            )

                        }
                    }
                }

            }

        }

        AddBottomSheet(
            onAddFile = onPickFile,
            onAddNote = viewModel::onAddNote,
            onAddManually = viewModel::onAddManually,
            onClose = viewModel::onAddBottomSheetCollapse,
            showBottomSheet = viewState.shouldShowAddBottomSheet
        )

        LongPressBottomSheet(
            layoutType = layoutType,
            longPressOptionsHolder = viewState.longPressOptionsHolder,
            onCollapse = viewModel::dismissBottomSheet,
            onOptionClick = viewModel::onLongPressOptionsItemSelected
        )

    }

}

@Composable
private fun ItemView(
    viewModel: AllItemsViewModel,
    viewState: AllItemsViewState,
    rItem: RItem,
    layoutType: CustomLayoutSize.LayoutType,
    showBottomDivider: Boolean = false
) {
    val model = viewState.itemKeyToItemCellModelMap[rItem.key]
    if (model == null) {
        AllItemsPlaceholderRow(layoutType = layoutType)
        return
    }
    Row(
        modifier = Modifier
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(),
                onClick = { viewModel.onItemTapped(rItem) },
                onLongClick = { viewModel.onItemLongTapped(rItem) }
            )
    ) {
        Spacer(modifier = Modifier.width(16.dp))
        Image(
            modifier = Modifier
                .size(layoutType.calculateItemsRowMainIconSize())
                .align(CenterVertically),
            painter = painterResource(id = model.typeIconName),
            contentDescription = null,
        )
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Column(
                    modifier = Modifier
                        .weight(1f)
                ) {
                    Text(
                        text = if (model.title.isEmpty()) " " else model.title,
                        fontSize = layoutType.calculateItemsRowTextSize(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row {
                        var subtitleText = if (model.subtitle.isEmpty()) " " else model.subtitle
                        val shouldHideSubtitle =
                            model.subtitle.isEmpty() && (model.hasNote || !model.tagColors.isEmpty())
                        if (shouldHideSubtitle) {
                            subtitleText = ""
                        }
                        Text(
                            text = subtitleText,
                            fontSize = layoutType.calculateItemsRowTextSize(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = CustomPalette.LightCharcoal,
                        )
                        if (model.hasNote) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Image(
                                modifier = Modifier
                                    .size(layoutType.calculateItemsRowNoteIconSize())
                                    .align(CenterVertically),
                                painter = painterResource(id = Drawables.item_note),
                                contentDescription = null,
                            )
                        }

                    }
                }
                setAccessory(accessory = model.accessory, layoutType = layoutType)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    modifier = Modifier
                        .size(layoutType.calculateItemsRowInfoIconSize())
                        .align(CenterVertically)
                        .safeClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = rememberRipple(),
                            onClick = {
                                viewModel.onAccessoryTapped(rItem)
                            }
                        ),
                    painter = painterResource(id = Drawables.ic_exclamation_24dp),
                    contentDescription = null,
                    tint = CustomTheme.colors.zoteroBlueWithDarkMode
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (showBottomDivider) {
                CustomDivider()
            }
        }
    }
}

@Composable
private fun RowScope.setAccessory(
    accessory: ItemCellModel.Accessory?,
    layoutType: CustomLayoutSize.LayoutType
) {
    if (accessory == null) {
        return
    }
    Spacer(modifier = Modifier.width(8.dp))
    when (accessory) {
        is ItemCellModel.Accessory.attachment -> {
            FileAttachmentView(
                modifier = Modifier
                    .size(layoutType.calculateItemsRowAccessoryIconSize())
                    .align(CenterVertically),
                state = accessory.state,
                style = Style.list,
            )
        }

        is ItemCellModel.Accessory.doi, is ItemCellModel.Accessory.url -> {
            Image(
                modifier = Modifier
                    .size(layoutType.calculateItemsRowAccessoryIconSize())
                    .align(CenterVertically),
                painter = painterResource(id = Drawables.list_link),
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun TopBar(
    onCloseClicked: () -> Unit,
    onAddClicked: () -> Unit,
    title: String,
) {
    CloseIconTopBar(
        title = title,
        onClose = onCloseClicked,
        actions = {
            HeadingTextButton(
                isEnabled = true,
                onClick = onAddClicked,
                text = stringResource(Strings.add)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
    )
}

@Composable
internal fun AddBottomSheet(
    onAddFile:() -> Unit,
    onAddNote:() -> Unit,
    onAddManually:() -> Unit,
    onClose: () -> Unit,
    showBottomSheet: Boolean,
) {

    var shouldShow by remember { mutableStateOf(false) }
    LaunchedEffect(showBottomSheet) {
        if (showBottomSheet) {
            shouldShow = true
        }
    }

    if (shouldShow) {
        CustomModalBottomSheet(
            shouldCollapse = !showBottomSheet,
            sheetContent = {
                AddBottomSheetContent(onAddFile = {
                    onClose()
                    onAddFile()
                }, onAddNote = {
                    onClose()
                    onAddNote()
                },
                onAddManually = {
                    onClose()
                    onAddManually()
                })
            },
            onCollapse = {
                shouldShow = false
                onClose()
            },
        )
    }

}

@Composable
private fun AddBottomSheetContent(
    onAddFile: () -> Unit,
    onAddNote: () -> Unit,
    onAddManually: () -> Unit,
) {
    Box {
        Column(
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            RowItemWithArrow(
                title = stringResource(id = Strings.add_item_bottom_sheet_manual),
                onClick = { onAddManually() }
            )
            CustomDivider(modifier = Modifier.padding(2.dp))
            RowItemWithArrow(
                title = stringResource(id = Strings.add_item_bottom_sheet_note),
                onClick = { onAddNote() }
            )
            CustomDivider(modifier = Modifier.padding(2.dp))
            RowItemWithArrow(
                title = stringResource(id = Strings.add_item_bottom_sheet_file),
                onClick = { onAddFile() }
            )
        }
    }
}

@Composable
private fun AllItemsPlaceholderRow(
    layoutType: CustomLayoutSize.LayoutType,
    showBottomDivider: Boolean = false
) {
    Row {
        Spacer(modifier = Modifier.width(16.dp))
        IconLoadingPlaceholder(
            modifier = Modifier
                .size(layoutType.calculateItemsRowMainIconSize())
                .align(CenterVertically),
        )
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Column(
                    modifier = Modifier
                        .weight(1f)
                ) {
                    TextLoadingPlaceholder(
                        modifier = Modifier
                            .height(layoutType.calculateItemsRowPlaceholderSize())
                            .fillMaxWidth(0.8f),
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row {
                        TextLoadingPlaceholder(
                            modifier = Modifier
                                .height(layoutType.calculateItemsRowPlaceholderSize())
                                .fillMaxWidth(0.4f),
                        )
                    }
                }
                IconLoadingPlaceholder(
                    modifier = Modifier
                        .size(layoutType.calculateItemsRowInfoIconSize())
                        .align(CenterVertically)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (showBottomDivider) {
                CustomDivider()
            }
        }
    }
}

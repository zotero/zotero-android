@file:OptIn(ExperimentalPagerApi::class)

package org.zotero.android.screens.tagpicker

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.checkbox.CircleCheckBox
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.misc.CustomDivider
import org.zotero.android.uicomponents.row.BaseRowItem
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.topbar.CancelSaveTitleTopBar

@Composable
@Suppress("UNUSED_PARAMETER")
internal fun TagPickerScreen(
    onBack: () -> Unit,
    scaffoldModifier: Modifier = Modifier,
    viewModel: TagPickerViewModel = hiltViewModel(),
) {
    val layoutType = CustomLayoutSize.calculateLayoutType()
    val viewState by viewModel.viewStates.observeAsState(TagPickerViewState())
    val viewEffect by viewModel.viewEffects.observeAsState()
    LaunchedEffect(key1 = viewModel) {
        viewModel.init()
    }

    LaunchedEffect(key1 = viewEffect) {
        when (val consumedEffect = viewEffect?.consume()) {
            null -> Unit
            is TagPickerViewEffect.OnBack -> {
                onBack()
            }
        }
    }
    CustomScaffold(
        modifier = scaffoldModifier,
        backgroundColor = CustomTheme.colors.popupBackgroundContent,
        topBar = {
            TopBar(
                onCancelClicked = onBack,
                onSave = viewModel::onSave,
                viewState = viewState,
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier
        ) {
            items(items = viewState.tags) { tag ->
                val isChecked = viewState.selectedTags.contains(tag.name)
                val backgroundColor =
                    if (isChecked) CustomTheme.colors.popupSelectedRow else CustomTheme.colors.popupBackgroundContent

                val columnModifier = Modifier
                    .safeClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { viewModel.selectOrDeselect(tag.name) }
                    )
                    .background(color = backgroundColor)

                Column(modifier = columnModifier) {
                    Spacer(modifier = Modifier.height(8.dp))
                    BaseRowItem(
                        modifier = Modifier.padding(start = 16.dp),
                        title = tag.name,
                        heightIn = 24.dp,
                        startContentPadding = 12.dp,
                        backgroundColor = backgroundColor,
                        textColor = CustomTheme.colors.primaryContent,
                        titleStyle = CustomTheme.typography.default.copy(fontSize = layoutType.calculateTextSize()),
                        startContent = {
                            CircleCheckBox(
                                isChecked = isChecked,
                                layoutType = layoutType
                            )
                        })
                    Spacer(modifier = Modifier.height(8.dp))
                    CustomDivider()
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    onCancelClicked: () -> Unit,
    onSave: () -> Unit,
    viewState: TagPickerViewState
) {
    CancelSaveTitleTopBar(
        title = stringResource(id = Strings.num_of_selected, viewState.selectedTags.size),
        onCancel = onCancelClicked,
        onSave = onSave,
        backgroundColor = CustomTheme.colors.popupBackgroundContent,
    )
}

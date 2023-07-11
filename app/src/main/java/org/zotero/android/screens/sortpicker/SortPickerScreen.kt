package org.zotero.android.screens.sortpicker

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.architecture.ui.CustomLayoutSize.LayoutType
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.misc.CustomDivider
import org.zotero.android.uicomponents.selector.MultiSelector
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.topbar.CancelSaveTitleTopBar

@Composable
@Suppress("UNUSED_PARAMETER")
internal fun SortPickerScreen(
    onBack: () -> Unit,
    navigateToSinglePickerScreen: () -> Unit,
    viewModel: SortPickerViewModel = hiltViewModel(),
) {
    val layoutType = CustomLayoutSize.calculateLayoutType()
    val viewState by viewModel.viewStates.observeAsState(SortPickerViewState())
    val viewEffect by viewModel.viewEffects.observeAsState()
    LaunchedEffect(key1 = viewModel) {
        viewModel.init()
    }

    LaunchedEffect(key1 = viewEffect) {
        when (viewEffect?.consume()) {
            null -> Unit
            is SortPickerViewEffect.OnBack -> {
                onBack()
            }
            is SortPickerViewEffect.NavigateToSinglePickerScreen -> {
                navigateToSinglePickerScreen()
            }
        }
    }
    CustomScaffold(
        topBar = {
            TopBar(
                onDone = viewModel::onDone,
            )
        },
    ) {
        Column(
            modifier = Modifier
                .background(color = CustomTheme.colors.surface)
        ) {
            DisplayFields(
                viewState = viewState,
                layoutType = layoutType,
                viewModel = viewModel,
            )
        }
    }
}

@Composable
private fun ColumnScope.DisplayFields(
    viewState: SortPickerViewState,
    viewModel: SortPickerViewModel,
    layoutType: LayoutType
) {
    Spacer(modifier = Modifier.height(20.dp))
    CustomDivider()
    FieldTappableRow(
        detailTitle = stringResource(id = Strings.sort_by, viewState.sortByTitle),
        layoutType = layoutType,
        onClick = viewModel::onSortFieldClicked
    )
    Spacer(modifier = Modifier.height(20.dp))
    val ascendingString = stringResource(id = Strings.ascending)
    val descendingString = stringResource(id = Strings.descending)
    MultiSelector(
        modifier = Modifier
            .padding(all = 16.dp)
            .fillMaxWidth()
            .height(layoutType.calculateSelectorHeight()),
        options = listOf(
            ascendingString,
            descendingString
        ),
        selectedOption = if (viewState.isAscending)
            ascendingString
        else descendingString,
        onOptionSelect = { viewModel.onSortDirectionChanged(it == ascendingString) },
        fontSize = layoutType.calculateTextSize(),
    )
}

@Composable
private fun FieldTappableRow(
    detailTitle: String,
    layoutType: LayoutType,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.safeClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            Text(
                modifier = Modifier.align(Alignment.Start).padding(start = 16.dp),
                text = detailTitle,
                color = CustomTheme.colors.secondaryContent,
                style = CustomTheme.typography.default,
                fontSize = layoutType.calculateTextSize(),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        CustomDivider()
    }
}


@Composable
private fun TopBar(
    onDone: () -> Unit,
) {
    CancelSaveTitleTopBar(
        onDone = onDone,
    )
}



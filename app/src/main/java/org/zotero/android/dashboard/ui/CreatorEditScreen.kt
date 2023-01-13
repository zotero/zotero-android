@file:OptIn(ExperimentalPagerApi::class)

package org.zotero.android.dashboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.architecture.ui.CustomLayoutSize.LayoutType
import org.zotero.android.dashboard.CreatorEditViewEffect
import org.zotero.android.dashboard.CreatorEditViewModel
import org.zotero.android.dashboard.CreatorEditViewState
import org.zotero.android.dashboard.ItemDetailsViewModel
import org.zotero.android.dashboard.data.ItemDetailCreator
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.misc.CustomDivider
import org.zotero.android.uicomponents.textinput.CustomTextField
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.topbar.CloseIconTopBar
import org.zotero.android.uicomponents.topbar.HeadingTextButton

@Composable
@Suppress("UNUSED_PARAMETER")
internal fun CreatorEditScreen(
    onBack: () -> Unit,
    viewModel: CreatorEditViewModel = hiltViewModel(),
    parentViewModel: ItemDetailsViewModel = hiltViewModel(),
) {
    val layoutType = CustomLayoutSize.calculateLayoutType()
    val viewState by viewModel.viewStates.observeAsState(CreatorEditViewState())
    val viewEffect by viewModel.viewEffects.observeAsState()
    LaunchedEffect(key1 = viewModel) {
        viewModel.init()
    }

    LaunchedEffect(key1 = viewEffect) {
        when (val consumedEffect = viewEffect?.consume()) {
            null -> Unit
            is CreatorEditViewEffect.OnCreatorCreated -> {
                parentViewModel.onSaveCreator(consumedEffect.itemDetailCreator)
                onBack()
            }
        }
    }
    CustomScaffold(
        topBar = {
            TopBar(
                onCloseClicked = onBack,
                onSave = viewModel::onSave,
                viewState = viewState,
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color = CustomTheme.colors.surface),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight(),
            ) {
                displayFields(
                    viewState = viewState,
                    layoutType = layoutType,
                    viewModel = viewModel,
                )
                Text(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .fillMaxWidth()
                        .safeClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = rememberRipple(),
                            onClick = viewModel::toggleNamePresentation
                        ),
                    text = stringResource(
                        id = if (viewState.creator?.namePresentation == ItemDetailCreator.NamePresentation.full)
                            Strings.switch_to_two_field else Strings.switch_to_single_field
                    ),
                    color = CustomTheme.colors.zoteroBlueWithDarkMode,
                    style = CustomTheme.typography.default,
                    fontSize = layoutType.calculateTextSize(),
                )
            }
        }
        SinglePickerViewBottomSheet(
            singlePickerState = viewState.singlePickerState,
            onOptionSelected = viewModel::onCreatorTypeSelected,
            onClose = viewModel::onCreatorTypeSheetCollapse,
            showBottomSheet = viewState.shouldShowCreatorTypeBottomSheet
        )
    }
}

@Composable
private fun displayFields(
    viewState: CreatorEditViewState,
    viewModel: CreatorEditViewModel,
    layoutType: LayoutType
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp)
    ) {
        LazyColumn {
            item {
                FieldTappableRow(
                    detailTitle = stringResource(id = Strings.creator_type),
                    detailValue = viewState.creator?.localizedType ?: "",
                    layoutType = layoutType,
                    onClick = viewModel::onCreatorTypeClicked
                )
                if (viewState.creator?.namePresentation == ItemDetailCreator.NamePresentation.separate) {
                    FieldEditableRow(
                        detailTitle = stringResource(id = Strings.last_name),
                        detailValue = viewState.creator?.lastName ?: "",
                        onValueChange = viewModel::onLastNameChange,
                        layoutType = layoutType
                    )
                    FieldEditableRow(
                        detailTitle = stringResource(id = Strings.first_name),
                        detailValue = viewState.creator?.firstName ?: "",
                        onValueChange = viewModel::onFirstNameChange,
                        layoutType = layoutType
                    )
                } else {
                    FieldEditableRow(
                        detailTitle = stringResource(id = Strings.name),
                        detailValue = viewState.creator?.fullName ?: "",
                        onValueChange = viewModel::onFullNameChange,
                        layoutType = layoutType
                    )
                }

            }
        }

    }

}

@Composable
private fun FieldEditableRow(
    detailTitle: String,
    detailValue: String,
    layoutType: LayoutType,
    textColor: Color = CustomTheme.colors.primaryContent,
    onValueChange: (String) -> Unit,
) {
    Row {
        Column(modifier = Modifier
            .padding(start = 12.dp)
            .width(90.dp)) {
            Text(
                modifier = Modifier.align(Alignment.Start),
                text = detailTitle,
                color = CustomTheme.colors.secondaryContent,
                style = CustomTheme.typography.default,
                fontSize = layoutType.calculateTextSize(),
            )
        }

        Column(modifier = Modifier.padding(start = 12.dp)) {
            CustomTextField(
                modifier = Modifier
                    .fillMaxSize(),
                value = detailValue,
                hint = "",
                textColor = textColor,
                onValueChange = onValueChange,
                textStyle = CustomTheme.typography.default,
            )
        }
    }
    CustomDivider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .height(2.dp)
    )
}

@Composable
private fun FieldTappableRow(
    detailTitle: String,
    detailValue: String,
    layoutType: LayoutType,
    onClick: () ->Unit,
    textColor: Color = CustomTheme.colors.primaryContent,
) {
    Row(
        modifier = Modifier.safeClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    ) {
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .width(90.dp),
        ) {
            Text(
                modifier = Modifier.align(Alignment.Start),
                text = detailTitle,
                color = CustomTheme.colors.secondaryContent,
                style = CustomTheme.typography.default,
                fontSize = layoutType.calculateTextSize(),
            )
        }

        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                modifier = Modifier,
                text = detailValue,
                color = textColor,
                style = CustomTheme.typography.default,
                fontSize = layoutType.calculateTextSize(),
            )
        }
    }
    CustomDivider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .height(2.dp)
    )
}


@Composable
private fun TopBar(
    onCloseClicked: () -> Unit,
    onSave: () -> Unit,
    viewState: CreatorEditViewState
) {
    CloseIconTopBar(
        title = viewState.creator?.localizedType,
        onClose = onCloseClicked,
        actions = {
            HeadingTextButton(
                onClick = onSave,
                text = stringResource(Strings.save),
                isEnabled = viewState.isValid
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
    )
}

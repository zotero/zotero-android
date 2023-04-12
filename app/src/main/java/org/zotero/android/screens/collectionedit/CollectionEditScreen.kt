package org.zotero.android.screens.collectionedit

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.architecture.ui.CustomLayoutSize.LayoutType
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.textinput.CustomTextField
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.topbar.CancelSaveTitleTopBar

@Composable
@Suppress("UNUSED_PARAMETER")
internal fun CollectionEditScreen(
    onBack: () -> Unit,
    navigateToLibraryPickerScreen: () -> Unit,
    scaffoldModifier: Modifier,
    viewModel: CollectionEditViewModel = hiltViewModel(),
) {
    val layoutType = CustomLayoutSize.calculateLayoutType()
    val viewState by viewModel.viewStates.observeAsState(CollectionEditViewState())
    val viewEffect by viewModel.viewEffects.observeAsState()
    LaunchedEffect(key1 = viewModel) {
        viewModel.init()
    }

    LaunchedEffect(key1 = viewEffect) {
        when (val consumedEffect = viewEffect?.consume()) {
            null -> Unit
            is CollectionEditViewEffect.OnBack -> {
                onBack()
            }
            is CollectionEditViewEffect.NavigateToLibraryPickerScreen -> {
                navigateToLibraryPickerScreen()
            }
        }
    }
    CustomScaffold(
        modifier = scaffoldModifier,
        topBar = {
            TopBar(
                onCancel = onBack,
                onSave = viewModel::onSave,
                viewState = viewState,
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(color = CustomTheme.colors.zoteroItemDetailSectionBackground)
                .padding(horizontal = 20.dp)
        ) {
            displayFields(
                viewState = viewState,
                layoutType = layoutType,
                viewModel = viewModel,
            )
        }
    }
}

private fun LazyListScope.displayFields(
    viewState: CollectionEditViewState,
    viewModel: CollectionEditViewModel,
    layoutType: LayoutType
) {
    item {
        Spacer(modifier = Modifier.height(40.dp))
        FieldEditableRow(
            detailValue = viewState.name,
            onValueChange = viewModel::onNameChanged,
            layoutType = layoutType
        )
    }
    item {
        Spacer(modifier = Modifier.height(20.dp))
        FieldTappableRow(
            viewState = viewState,
            layoutType = layoutType,
            onClick = viewModel::onParentClicked
        )
        Spacer(modifier = Modifier.height(16.dp))
    }

}

@Composable
private fun FieldEditableRow(
    detailValue: String,
    layoutType: LayoutType,
    textColor: Color = CustomTheme.colors.primaryContent,
    onValueChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier.background(
            color = CustomTheme.colors.zoteroEditFieldBackground,
            shape = RoundedCornerShape(size = 10.dp)
        )
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        CustomTextField(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 8.dp),
            value = detailValue,
            maxLines = 1,
            hint = stringResource(id = Strings.name),
            textColor = textColor,
            onValueChange = onValueChange,
            textStyle = CustomTheme.typography.default.copy(fontSize = layoutType.calculateTextSize()),
        )
        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
private fun FieldTappableRow(
    viewState: CollectionEditViewState,
    layoutType: LayoutType,
    onClick: () ->Unit,
) {
    Column(
        modifier = Modifier
            .background(
                color = CustomTheme.colors.zoteroEditFieldBackground,
                shape = RoundedCornerShape(size = 10.dp)
            )
            .safeClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(),
                onClick = onClick
            )
    ) {
//        Spacer(modifier = Modifier.height(5.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                modifier = Modifier
                    .size(layoutType.calculateIconSize()),
                painter = painterResource(
                    id = if (viewState.parent == null) {
                        Drawables.icon_cell_library
                    } else {
                        Drawables.icon_cell_collection
                    }
                ),
                contentDescription = null,
                tint = CustomTheme.colors.zoteroBlueWithDarkMode
            )

            Text(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                text = viewState.parent?.name ?: viewState.library.name,
                fontSize = layoutType.calculateTextSize(),
                color = CustomTheme.colors.primaryContent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
//        Spacer(modifier = Modifier.height(5.dp))
    }

}


@Composable
private fun TopBar(
    onCancel: () -> Unit,
    onSave: () -> Unit,
    viewState: CollectionEditViewState
) {
    CancelSaveTitleTopBar(
        title = stringResource(id = if(viewState.key != null) Strings.edit_collection else Strings.create_collection),
        onCancel = onCancel,
        onSave = onSave,
    )

}

package org.zotero.android.screens.collectionedit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.architecture.ui.CustomLayoutSize.LayoutType
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.topbar.CancelSaveTitleTopBar

@Composable
internal fun CollectionEditScreen(
    onBack: () -> Unit,
    navigateToCollectionPickerScreen: () -> Unit,
    viewModel: CollectionEditViewModel = hiltViewModel(),
) {
    val layoutType = CustomLayoutSize.calculateLayoutType()
    val viewState by viewModel.viewStates.observeAsState(CollectionEditViewState())
    val viewEffect by viewModel.viewEffects.observeAsState()
    LaunchedEffect(key1 = viewModel) {
        viewModel.init()
    }

    LaunchedEffect(key1 = viewEffect) {
        when (viewEffect?.consume()) {
            is CollectionEditViewEffect.OnBack -> {
                onBack()
            }
            is CollectionEditViewEffect.NavigateToCollectionPickerScreen -> {
                navigateToCollectionPickerScreen()
            }
            else -> {}
        }
    }
    CustomScaffold(
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
            collectionEditRows(
                viewState = viewState,
                layoutType = layoutType,
                viewModel = viewModel,
            )
        }
        val error = viewState.error
        if (error != null) {
            CollectionEditErrorDialogs(
                error = error,
                onDismissErrorDialog = viewModel::onDismissErrorDialog,
                deleteOrRestoreItem = viewModel::deleteOrRestoreCollection
            )
        }
    }
}

private fun LazyListScope.collectionEditRows(
    viewState: CollectionEditViewState,
    viewModel: CollectionEditViewModel,
    layoutType: LayoutType
) {
    item {
        Spacer(modifier = Modifier.height(30.dp))
        CollectionEditFieldEditableRow(
            detailValue = viewState.name,
            viewModel  = viewModel,
            layoutType = layoutType
        )
    }
    item {
        Spacer(modifier = Modifier.height(30.dp))
        LibrarySelectorRow(
            viewState = viewState,
            layoutType = layoutType,
            onClick = viewModel::onParentClicked
        )
        if (viewState.key != null) {
            Spacer(modifier = Modifier.height(30.dp))
            CollectionEditFieldTappableRow(
                title = stringResource(id = Strings.collections_delete),
                layoutType = layoutType,
                onClick = viewModel::delete
            )
            Spacer(modifier = Modifier.height(10.dp))
            CollectionEditFieldTappableRow(
                title = stringResource(id = Strings.collections_delete_with_items),
                layoutType = layoutType,
                onClick = viewModel::deleteWithItems
            )
        }
    }

}

@Composable
private fun TopBar(
    onCancel: () -> Unit,
    onSave: () -> Unit,
    viewState: CollectionEditViewState
) {
    CancelSaveTitleTopBar(
        title = stringResource(id = if(viewState.key != null) Strings.collections_edit_title else Strings.collections_create_title),
        onCancel = onCancel,
        onSave = onSave,
        isSaveButtonEnabled = viewState.isValid
    )
}

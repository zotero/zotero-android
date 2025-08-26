package org.zotero.android.screens.collectionedit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.screens.settings.elements.NewSettingsDivider
import org.zotero.android.screens.settings.elements.NewSettingsItem
import org.zotero.android.screens.settings.elements.NewSettingsSectionTitle
import org.zotero.android.uicomponents.CustomScaffoldM3
import org.zotero.android.uicomponents.Strings

@Composable
internal fun CollectionEditScreen(
    onBack: () -> Unit,
    navigateToCollectionPickerScreen: () -> Unit,
    viewModel: CollectionEditViewModel = hiltViewModel(),
) {
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
    CustomScaffoldM3(
        topBar = {
            CollectionEditTopBar(
                onCancel = onBack,
                onSave = viewModel::onSave,
                viewState = viewState,
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            CollectionEditRows(
                viewState = viewState,
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

@Composable
private fun CollectionEditRows(
    viewState: CollectionEditViewState,
    viewModel: CollectionEditViewModel,
) {
    NewSettingsSectionTitle(titleId = Strings.name)
    CollectionEditFieldEditableRow(
        detailValue = viewState.name,
        viewModel = viewModel,
    )
    NewSettingsDivider()
    NewSettingsSectionTitle(titleId = Strings.collection_edit_parent)
    LibrarySelectorRow(
        viewState = viewState,
        onClick = viewModel::onParentClicked
    )
    if (viewState.key != null) {
        NewSettingsDivider()
        NewSettingsItem(
            textColor = MaterialTheme.colorScheme.error,
            title = stringResource(id = Strings.collections_delete),
            onItemTapped = viewModel::delete
        )
        NewSettingsItem(
            textColor = MaterialTheme.colorScheme.error,
            title = stringResource(id = Strings.collections_delete_with_items),
            onItemTapped = viewModel::deleteWithItems
        )
    }

}
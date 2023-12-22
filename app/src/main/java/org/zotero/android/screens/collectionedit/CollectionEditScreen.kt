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
import org.zotero.android.screens.settings.SettingsDivider
import org.zotero.android.screens.settings.SettingsItem
import org.zotero.android.screens.settings.SettingsSection
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.theme.CustomPalette
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.theme.CustomThemeWithStatusAndNavBars

@Composable
internal fun CollectionEditScreen(
    onBack: () -> Unit,
    navigateToCollectionPickerScreen: () -> Unit,
    viewModel: CollectionEditViewModel = hiltViewModel(),
) {
    CustomThemeWithStatusAndNavBars(
        statusBarBackgroundColor = CustomTheme.colors.topBarBackgroundColor,
        navBarBackgroundColor = CustomTheme.colors.zoteroItemDetailSectionBackground
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
        CustomScaffold(
            topBar = {
                CollectionEditTopBar(
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
                    .padding(horizontal = 16.dp)
            ) {
                collectionEditRows(
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
}

private fun LazyListScope.collectionEditRows(
    viewState: CollectionEditViewState,
    viewModel: CollectionEditViewModel,
) {
    item {
        Spacer(modifier = Modifier.height(30.dp))
        CollectionEditFieldEditableRow(
            detailValue = viewState.name,
            viewModel = viewModel,
        )
    }
    item {
        Spacer(modifier = Modifier.height(30.dp))
        LibrarySelectorRow(
            viewState = viewState,
            onClick = viewModel::onParentClicked
        )
        if (viewState.key != null) {
            Spacer(modifier = Modifier.height(30.dp))
            SettingsSection {
                SettingsItem(
                    textColor = CustomPalette.ErrorRed,
                    title = stringResource(id = Strings.collections_delete),
                    onItemTapped = viewModel::delete
                )
                SettingsDivider()
                SettingsItem(
                    textColor = CustomPalette.ErrorRed,
                    title = stringResource(id = Strings.collections_delete_with_items),
                    onItemTapped = viewModel::deleteWithItems
                )
            }
        }
    }

}
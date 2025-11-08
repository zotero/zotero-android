package org.zotero.android.screens.settings

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.screens.dashboard.BuildInfo
import org.zotero.android.screens.settings.elements.NewSettingsDivider
import org.zotero.android.screens.settings.elements.NewSettingsItem
import org.zotero.android.screens.settings.quickcopy.SettingsQuickCopySwitchItem
import org.zotero.android.uicomponents.CustomScaffoldM3
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.themem3.AppThemeM3


@Composable
internal fun SettingsScreen(
    onBack: () -> Unit,
    onOpenWebpage: (uri: Uri) -> Unit,
    toAccountScreen: () -> Unit,
    toDebugScreen: () -> Unit,
    toCiteScreen: () -> Unit,
    toQuickCopyScreen: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    AppThemeM3 {
        val viewEffect by viewModel.viewEffects.observeAsState()
        val viewState by viewModel.viewStates.observeAsState(SettingsViewState())
        LaunchedEffect(key1 = viewModel) {
            viewModel.init()
        }

        LaunchedEffect(key1 = viewEffect) {
            when (val consumedEffect = viewEffect?.consume()) {
                null -> Unit
                is SettingsViewEffect.OnBack -> {
                    onBack()
                }

                is SettingsViewEffect.OpenWebpage -> {
                    onOpenWebpage(consumedEffect.uri)
                }
            }
        }
        CustomScaffoldM3(
            topBar = {
                SettingsTopBar(
                    onClose = onBack,
                )
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Spacer(modifier = Modifier.height(4.dp))
                NewSettingsItem(
                    title = stringResource(id = Strings.settings_sync_account),
                    onItemTapped = toAccountScreen,
                )

                NewSettingsDivider()

                NewSettingsItem(
                    title = stringResource(id = Strings.settings_export_title),
                    onItemTapped = toQuickCopyScreen,
                )
                NewSettingsItem(
                    title = stringResource(id = Strings.settings_cite_title),
                    onItemTapped = toCiteScreen,
                )
                NewSettingsItem(
                    title = stringResource(id = Strings.settings_debug),
                    onItemTapped = toDebugScreen,
                )

                NewSettingsDivider()

                NewSettingsItem(
                    title = stringResource(id = Strings.support_feedback),
                    onItemTapped = viewModel::openSupportAndFeedback
                )
                NewSettingsItem(
                    title = stringResource(id = Strings.privacy_policy),
                    onItemTapped = viewModel::openPrivacyPolicy
                )

                NewSettingsDivider()


                SettingsQuickCopySwitchItem(
                    title = stringResource(id = Strings.settings_show_subcollection_items),
                    isChecked = viewState.showSubcollectionItems,
                    onCheckedChange = viewModel::onShowSubcollectionItemsChanged
                )

                NewSettingsDivider()
                BuildInfo()
            }
        }
    }
}
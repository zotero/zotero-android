package org.zotero.android.screens.settings.account

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.screens.settings.SettingsItem
import org.zotero.android.screens.settings.SettingsSection
import org.zotero.android.screens.settings.SettingsSectionTitle
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.theme.CustomPalette
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.theme.CustomThemeWithStatusAndNavBars

@Composable
internal fun SettingsAccountScreen(
    onBack: () -> Unit,
    onOpenWebpage: (uri: Uri) -> Unit,
    viewModel: SettingsAccountViewModel = hiltViewModel(),
) {
    val backgroundColor = CustomTheme.colors.zoteroItemDetailSectionBackground
    CustomThemeWithStatusAndNavBars(
        navBarBackgroundColor = backgroundColor,
    ) {
        val layoutType = CustomLayoutSize.calculateLayoutType()
        val viewState by viewModel.viewStates.observeAsState(SettingsAccountViewState())
        val viewEffect by viewModel.viewEffects.observeAsState()
        LaunchedEffect(key1 = viewModel) {
            viewModel.init()
        }

        LaunchedEffect(key1 = viewEffect) {
            when (val consumedEffect = viewEffect?.consume()) {
                null -> Unit
                is SettingsAccountViewEffect.OnBack -> {
                    onBack()
                }

                is SettingsAccountViewEffect.OpenWebpage -> {
                    onOpenWebpage(consumedEffect.uri)
                }
            }
        }
        CustomScaffold(
            backgroundColor = CustomTheme.colors.popupBackgroundContent,
            topBar = {
                SettingsAccountTopBar(
                    onBack = onBack,
                )
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = backgroundColor)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(30.dp))
                SettingsSectionTitle(layoutType = layoutType, titleId = Strings.settings_data_sync)
                SettingsSection {
                    SettingsItem(
                        layoutType = layoutType,
                        isLastItem = false,
                        title = viewState.username,
                        onItemTapped = {}
                    )
                    SettingsItem(
                        layoutType = layoutType,
                        isLastItem = true,
                        textColor = CustomPalette.ErrorRed,
                        title = stringResource(id = Strings.settings_logout),
                        onItemTapped = viewModel::onSignOut
                    )
                }
                Spacer(modifier = Modifier.height(30.dp))
                SettingsSectionTitle(
                    layoutType = layoutType,
                    titleId = Strings.settings_account_caps
                )
                SettingsSection {
                    SettingsItem(
                        layoutType = layoutType,
                        isLastItem = false,
                        textColor = CustomTheme.colors.zoteroDefaultBlue,
                        title = stringResource(id = Strings.settings_sync_manage_account),
                        onItemTapped = viewModel::openManageAccount
                    )
                    SettingsItem(
                        layoutType = layoutType,
                        isLastItem = true,
                        textColor = CustomPalette.ErrorRed,
                        title = stringResource(id = Strings.settings_sync_delete_account),
                        onItemTapped = viewModel::openDeleteAccount
                    )
                }
            }
        }
    }
}
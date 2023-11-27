package org.zotero.android.screens.settings

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
import org.zotero.android.screens.dashboard.BuildInfo
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.theme.CustomThemeWithStatusAndNavBars

@Composable
internal fun SettingsScreen(
    onBack: () -> Unit,
    onOpenWebpage: (uri: Uri) -> Unit,
    toAccountScreen: () -> Unit,
    toDebugScreen: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val backgroundColor = CustomTheme.colors.zoteroItemDetailSectionBackground

    CustomThemeWithStatusAndNavBars(
        navBarBackgroundColor = backgroundColor,
    ) {
        val layoutType = CustomLayoutSize.calculateLayoutType()
        val viewEffect by viewModel.viewEffects.observeAsState()
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
        CustomScaffold(
            backgroundColor = CustomTheme.colors.popupBackgroundContent,
            topBar = {
                SettingsTopBar(
                    onClose = onBack,
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
                SettingsSection {
                    SettingsItem(
                        layoutType = layoutType,
                        isLastItem = true,
                        title = stringResource(id = Strings.settings_sync_account),
                        onItemTapped = toAccountScreen
                    )
                }
                Spacer(modifier = Modifier.height(30.dp))

                SettingsSection {
                    SettingsItem(
                        layoutType = layoutType,
                        isLastItem = true,
                        title = stringResource(id = Strings.settings_debug),
                        onItemTapped = toDebugScreen
                    )
                }
                Spacer(modifier = Modifier.height(30.dp))

                SettingsSection {
                    SettingsItem(
                        layoutType = layoutType,
                        isLastItem = false,
                        title = stringResource(id = Strings.support_feedback),
                        onItemTapped = viewModel::openSupportAndFeedback
                    )
                    SettingsItem(
                        layoutType = layoutType,
                        isLastItem = true,
                        title = stringResource(id = Strings.privacy_policy),
                        onItemTapped = viewModel::openPrivacyPolicy
                    )
                }
                BuildInfo()
            }
        }
    }
}
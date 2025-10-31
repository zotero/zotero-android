package org.zotero.android.screens.citation.singlecitation

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
import org.zotero.android.screens.citation.singlecitation.components.SingleCitationDropBoxAndEditFieldItem
import org.zotero.android.screens.citation.singlecitation.components.SingleCitationSwitchItem
import org.zotero.android.screens.citation.singlecitation.components.SingleCitationWebViewItem
import org.zotero.android.screens.settings.elements.NewSettingsDivider
import org.zotero.android.screens.settings.elements.NewSettingsSectionTitle
import org.zotero.android.uicomponents.CustomScaffoldM3
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.themem3.AppThemeM3

@Composable
internal fun SingleCitationScreen(
    onBack: () -> Unit,
    viewModel: SingleCitationViewModel = hiltViewModel(),
) {
    AppThemeM3 {
        val viewState by viewModel.viewStates.observeAsState(SingleCitationViewState())
        val viewEffect by viewModel.viewEffects.observeAsState()
        LaunchedEffect(key1 = viewModel) {
            viewModel.init()
        }

        LaunchedEffect(key1 = viewEffect) {
            when (viewEffect?.consume()) {
                is SingleCitationViewEffect.OnBack -> {
                    onBack()
                }

                else -> {
                    //no-op
                }
            }
        }
        CustomScaffoldM3(
            topBar = {
                SingleCitationTopBar(
                    onCancelClicked = onBack,
                    onCopyTapped = viewModel::onCopyTapped,
                )
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                SingleCitationDropBoxAndEditFieldItem(viewState, viewModel)
                SingleCitationSwitchItem(
                    title = stringResource(Strings.citation_omit_author),
                    isChecked = viewState.omitAuthor,
                    onCheckedChange = viewModel::onOmitAuthor
                )

                NewSettingsDivider()
                NewSettingsSectionTitle(titleId = Strings.citation_preview)
                Spacer(modifier = Modifier.height(8.dp))
                SingleCitationWebViewItem(
                    previewHtml = viewState.preview,
                    height = viewState.previewHeight
                )
            }
        }
    }
}
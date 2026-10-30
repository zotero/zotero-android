package org.zotero.android.screens.reader.readaloud

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.screens.reader.readaloud.voiceselection.ReadAloudVoiceSelectionState
import org.zotero.android.screens.reader.readaloud.voiceselection.ReadAloudVoiceSelectionViewEffect
import org.zotero.android.screens.reader.readaloud.voiceselection.ReadAloudVoiceSelectionViewModel
import org.zotero.android.uicomponents.CustomScaffoldM3
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.themem3.AppThemeM3

@Composable
internal fun ReadAloudOnboardingScreen(
    onBack: () -> Unit,
    viewModel: ReadAloudVoiceSelectionViewModel = hiltViewModel(),
) {
    viewModel.init()
    val viewState by viewModel.viewStates.observeAsState(ReadAloudVoiceSelectionState())
    val viewEffect by viewModel.viewEffects.observeAsState()

    BackHandler(onBack = viewModel::onCancel)
    AppThemeM3(darkTheme = isSystemInDarkTheme()) {
        LaunchedEffect(key1 = viewEffect) {
            when (viewEffect?.consume()) {
                null -> Unit
                is ReadAloudVoiceSelectionViewEffect.OnBack -> onBack()
            }
        }
        CustomScaffoldM3(
            topBar = {
                OnboardingTopBar(onCancel = viewModel::onCancel, onDone = viewModel::onDone)
            },
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
                    text = stringResource(Strings.speech_onboarding_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                VoiceTypeToggle(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    selected = viewState.type,
                    onSelected = viewModel::onTypeSelected,
                )
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        DescriptionBullets(viewState)
                    }
                    if (viewState.currentRegions.size > 1) {
                        regionSection(viewState, viewModel::onRegionSelected)
                    }
                    voiceListSection(
                        state = viewState,
                        onVoiceSelected = viewModel::onVoiceSelected,
                        onRetry = viewModel::loadVoices,
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingTopBar(
    onCancel: () -> Unit,
    onDone: () -> Unit,
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        title = {},
        navigationIcon = {
            TextButton(onClick = onCancel) {
                Text(text = stringResource(Strings.cancel))
            }
        },
        actions = {
            TextButton(onClick = onDone) {
                Text(text = stringResource(Strings.done))
            }
        },
    )
}

@Composable
private fun DescriptionBullets(state: ReadAloudVoiceSelectionState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        state.type.descriptionRes().forEach { resId ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "•", color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = stringResource(resId),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

private fun LazyListScope.regionSection(
    state: ReadAloudVoiceSelectionState,
    onRegionSelected: (String) -> Unit,
) {
    item {
        Text(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
            text = stringResource(Strings.speech_region).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    items(state.currentRegions.size) { index ->
        val region = state.currentRegions[index]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onRegionSelected(region.locale) }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = region.displayName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (region.locale == state.selectedRegionLocale) {
                Icon(
                    painter = painterResource(Drawables.check_24px),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
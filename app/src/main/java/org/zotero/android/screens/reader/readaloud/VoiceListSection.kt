package org.zotero.android.screens.reader.readaloud

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.zotero.android.screens.reader.readaloud.voiceselection.ReadAloudVoiceSelectionState
import org.zotero.android.screens.reader.readaloud.voiceselection.ReadAloudVoiceType
import org.zotero.android.speech.RemainingTimeFormatter
import org.zotero.android.speech.SpeechVoice
import org.zotero.android.speech.data.RemoteVoice
import org.zotero.android.speech.identifier
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import java.util.Locale

internal fun LazyListScope.voiceListSection(
    state: ReadAloudVoiceSelectionState,
    onVoiceSelected: (SpeechVoice) -> Unit,
    onRetry: () -> Unit,
) {
    when (state.type) {
        ReadAloudVoiceType.local -> {
            val voices = state.currentLocalVoices
            if (voices.isEmpty()) {
                item { NoVoicesMessage(state.baseLanguage) }
                return
            }
            item { VoicesHeader() }
            items(voices.size) { index ->
                val voice = voices[index]
                val selected = (state.selectedVoice as? SpeechVoice.Local)?.voice?.identifier == voice.identifier
                VoiceRow(
                    title = voice.name,
                    isSelected = selected,
                    remainingTime = null,
                    onClick = { onVoiceSelected(SpeechVoice.Local(voice)) },
                )
            }
        }

        ReadAloudVoiceType.premium, ReadAloudVoiceType.standard -> when {
            state.loadError -> item { LoadError(onRetry) }
            state.voicesResponse != null -> {
                if (state.groupedRemoteVoices.isEmpty()) {
                    item { NoVoicesMessage(state.baseLanguage) }
                    return
                }
                item { VoicesHeader() }
                val voices = state.currentRemoteVoices
                items(voices.size) { index ->
                    val voice = voices[index]
                    val selected = (state.selectedVoice as? SpeechVoice.Remote)?.voice?.id == voice.id
                    VoiceRow(
                        title = voice.label,
                        isSelected = selected,
                        remainingTime = remainingTime(voice, state.displayedCreditsRemaining),
                        onClick = { onVoiceSelected(SpeechVoice.Remote(voice)) },
                    )
                }
            }

            state.isLoading -> item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}

private fun remainingTime(voice: RemoteVoice, creditsRemaining: Int?): Double? {
    if (creditsRemaining == null || voice.creditsPerMinute <= 0) {
        return null
    }
    val time = creditsRemaining.toDouble() / voice.creditsPerMinute * 60
    return time.takeIf { RemainingTimeFormatter.shouldDisplay(it) }
}

@Composable
private fun VoicesHeader() {
    Text(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
        text = stringResource(Strings.speech_voices).uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun VoiceRow(
    title: String,
    isSelected: Boolean,
    remainingTime: Double?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (remainingTime != null) {
            Text(
                text = RemainingTimeFormatter.formatted(remainingTime),
                style = MaterialTheme.typography.bodyMedium,
                color = if (RemainingTimeFormatter.isWarning(remainingTime)) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        if (isSelected) {
            Icon(
                painter = painterResource(Drawables.check_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun LoadError(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(Strings.errors_shareext_cant_load_data),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onRetry) {
            Text(text = stringResource(Strings.retry))
        }
    }
}

@Composable
private fun NoVoicesMessage(language: String) {
    val languageName = Locale.forLanguageTag(language).getDisplayName(Locale.getDefault()).ifEmpty { language }
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        text = stringResource(Strings.speech_no_voices_for_tier, languageName),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}
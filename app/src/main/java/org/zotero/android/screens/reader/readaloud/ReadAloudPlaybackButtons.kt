package org.zotero.android.screens.reader.readaloud

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.safeStringResource

@Composable
internal fun ReadAloudPlaybackButtons(
    controls: ReadAloudControlsState,
    onBackward: () -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onForward: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlaybackIconButton(
            drawableRes = Drawables.speech_backward,
            contentDescription = safeStringResource(Strings.accessibility_speech_backward),
            enabled = controls.areSkipButtonsEnabled,
            onClick = onBackward,
        )
        Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
            when {
                controls.isProgressVisible -> CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp,
                )

                controls.isPauseVisible -> PlaybackIconButton(
                    drawableRes = Drawables.speech_pause,
                    contentDescription = safeStringResource(Strings.accessibility_speech_pause),
                    enabled = true,
                    onClick = onPause,
                )

                controls.isPlayVisible -> PlaybackIconButton(
                    drawableRes = Drawables.speech_play,
                    contentDescription = safeStringResource(Strings.accessibility_speech_play),
                    enabled = true,
                    onClick = onPlay,
                )
            }
        }
        PlaybackIconButton(
            drawableRes = Drawables.speech_forward,
            contentDescription = safeStringResource(Strings.accessibility_speech_forward),
            enabled = controls.areSkipButtonsEnabled,
            onClick = onForward,
        )
    }
}

@Composable
private fun PlaybackIconButton(
    drawableRes: Int,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(
            painter = painterResource(drawableRes),
            contentDescription = contentDescription,
            tint = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            },
        )
    }
}
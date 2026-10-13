package org.zotero.android.screens.reader.readaloud

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.Drawables

private val sideSlotWidth = 88.dp

@Composable
internal fun ReadAloudControlsBar(
    controls: ReadAloudControlsState,
    onBackward: () -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onForward: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .navigationBarsPadding()
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.width(sideSlotWidth)) {
                controls.remainingTimeText?.let { RemainingTimeLabel(it) }
            }
            ReadAloudPlaybackButtons(
                controls = controls,
                onBackward = onBackward,
                onPlay = onPlay,
                onPause = onPause,
                onForward = onForward,
            )
            Spacer(modifier = Modifier.width(sideSlotWidth))
        }
    }
}

@Composable
private fun RemainingTimeLabel(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            modifier = Modifier.size(16.dp),
            painter = painterResource(Drawables.speech_clock),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
        )
        Text(
            modifier = Modifier.padding(start = 4.dp),
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }
}
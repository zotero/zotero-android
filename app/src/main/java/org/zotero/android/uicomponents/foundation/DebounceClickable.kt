package org.zotero.android.uicomponents.foundation

import androidx.compose.foundation.Indication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

@Composable
fun Modifier.debounceCombinedClickable(
    interactionSource: MutableInteractionSource?,
    indication: Indication?,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier {
    return this.combinedClickable(
        interactionSource = interactionSource,
        indication = indication,
        enabled = enabled,
        onClickLabel = null,
        role = null,
        onLongClickLabel = null,
        onLongClick = onLongClick,
        onDoubleClick = null,
        onClick = debounceInteraction(onClick)
    )
}


@Composable
fun Modifier.debounceClickable(
    interactionSource: MutableInteractionSource?,
    indication: Indication?,
    enabled: Boolean = true,
    onClick: () -> Unit,
    role: Role? = null,
): Modifier {
    return this.clickable(
        interactionSource = interactionSource,
        indication = indication,
        enabled = enabled,
        onClickLabel = null,
        role = role,
        onClick = debounceInteraction(onClick),
    )
}


@Composable
private fun debounceInteraction(onClick: () -> Unit): () -> Unit {
    val debounceInterval: Long = 600
    var lastClickTime by remember { mutableLongStateOf(0L) }
    val onClickDebounce = {
        val currentTime = System.currentTimeMillis()
        if ((currentTime - lastClickTime) >= debounceInterval) {
            lastClickTime = currentTime
            onClick()
        }
    }
    return onClickDebounce
}
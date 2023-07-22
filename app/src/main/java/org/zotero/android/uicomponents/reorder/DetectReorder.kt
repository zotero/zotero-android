package org.zotero.android.uicomponents.reorder

import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

fun Modifier.detectReorderAfterLongPress(state: ReorderableState) =
    this.then(
        Modifier.pointerInput(Unit) {
            forEachGesture {
                val down = awaitPointerEventScope {
                    awaitFirstDown(requireUnconsumed = true)
                }
                awaitLongPressOrCancellation(down)?.also {
                    state.ch.trySend(StartDrag(down.id))
                }
            }
        }
    )

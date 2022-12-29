package org.zotero.android.uicomponents.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import kotlinx.coroutines.delay

/**
 * A helper method to create a [FocusRequester] to be remembered. By using [requestOnLaunch] you
 * can trigger the keyboard to pop-up as soon as this Composable enters the tree.
 */
@Composable
fun rememberFocusRequester(
    requestOnLaunch: Boolean = false,
    requestDelay: Long = 0L,
): FocusRequester {
    val focusRequester = remember { FocusRequester() }
    if (requestOnLaunch) {
        LaunchedEffect(Unit) {
            delay(requestDelay)
            focusRequester.requestFocus()
        }
    }
    return focusRequester
}

@Composable
fun rememberFocusRequesterWithDelay() = rememberFocusRequester(
    requestOnLaunch = true,
    requestDelay = 100L
)

package org.zotero.android.uicomponents.foundation

import android.content.res.Resources
import androidx.annotation.PluralsRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

@Composable
@ReadOnlyComposable
@Suppress("SpreadOperator")
fun quantityStringResource(
    @PluralsRes id: Int,
    quantity: Int,
    vararg formatArgs: Any
): String {
    val resources = resources()
    val formatArgsArray = if (formatArgs.isEmpty()) {
        arrayOf(quantity)
    } else {
        formatArgs
    }
    return resources.getQuantityString(id, quantity, *formatArgsArray)
}

/**
 * A composable function that returns the [Resources]. It will be recomposed
 * when [Configuration] gets updated.
 */
@Composable
@ReadOnlyComposable
private fun resources(): Resources {
    LocalConfiguration.current
    return LocalContext.current.resources
}

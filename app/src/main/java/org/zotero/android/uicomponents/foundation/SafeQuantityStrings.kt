package org.zotero.android.uicomponents.foundation

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import timber.log.Timber

fun Context.getSafeQuantityString(
    @PluralsRes id: Int,
    quantity: Int,
    vararg formatArgs: Any,
): String {
    val formatArgsArray = if (formatArgs.isEmpty()) {
        arrayOf(quantity)
    } else {
        formatArgs
    }

    val currentLanguageTag = currentLanguageTag(resources.configuration)
    val idName = resources.getResourceEntryName(id)
    try {
        return getQuantityStringForContext(
            context = this,
            id = id,
            quantity = quantity,
            formatArgs = formatArgsArray
        )
    } catch (e: Exception) {
        Timber.e(e, "Unable to retrieve $idName for language $currentLanguageTag")
        try {
            val stringForEnglishContext = getQuantityStringForContext(
                context = getEnglishContext(this),
                id = id,
                quantity = quantity,
                formatArgs = formatArgsArray
            )
            return stringForEnglishContext
        } catch (e: Exception) {
            Timber.e(e, "Unable to retrieve $idName for forced English")
            return idName
        }
    }
}

@Composable
fun safeQuantityStringResource(
    @PluralsRes id: Int,
    quantity: Int,
    vararg formatArgs: Any
): String {
    val formatArgsArray = if (formatArgs.isEmpty()) {
        arrayOf(quantity)
    } else {
        formatArgs
    }

    val context = LocalContext.current
    val currentLanguageTag = currentLanguageTag(LocalConfiguration.current)
    val result = remember(id, currentLanguageTag, *formatArgsArray, quantity) {
        val idName = context.resources.getResourceEntryName(id)
        try {
            return@remember getQuantityStringForContext(
                context = context,
                id = id,
                quantity = quantity,
                formatArgs = formatArgsArray
            )
        } catch (e: Exception) {
            Timber.e(e, "Unable to retrieve $idName for language $currentLanguageTag")
            try {
                val stringForEnglishContext = getQuantityStringForContext(
                    context = getEnglishContext(context),
                    id = id,
                    quantity = quantity,
                    formatArgs = formatArgsArray
                )
                return@remember stringForEnglishContext
            } catch (e: Exception) {
                Timber.e(e, "Unable to retrieve $idName for forced English")
                return@remember idName
            }
        }
    }
    return result
}

private fun getQuantityStringForContext(
    context: Context,
    @PluralsRes id: Int,
    quantity: Int,
    vararg formatArgs: Any,
): String {
    return context.resources.getQuantityString(id, quantity, *formatArgs)
}

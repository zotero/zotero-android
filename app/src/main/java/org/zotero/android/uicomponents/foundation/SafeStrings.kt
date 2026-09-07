package org.zotero.android.uicomponents.foundation

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import timber.log.Timber
import java.util.Locale

fun Context.getSafeString(@StringRes id: Int, vararg formatArgs: Any): String {
    val currentLanguageTag = currentLanguageTag(this.resources.configuration)
    val idName = resources.getResourceEntryName(id)
    try {
        return getStringForContext(context = this, id = id, formatArgs = formatArgs)
    } catch (e: Exception) {
        Timber.e(e, "Unable to retrieve $idName for language $currentLanguageTag")
        try {
            val stringForEnglishContext = getStringForContext(
                context = getEnglishContext(this),
                id = id,
                formatArgs = formatArgs
            )
            return stringForEnglishContext
        } catch (e: Exception) {
            Timber.e(e, "Unable to retrieve $idName for forced English")
            return idName
        }
    }
}

@Composable
fun safeStringResource(@StringRes id: Int, vararg formatArgs: Any): String {
    val context = LocalContext.current
    val currentLanguageTag = currentLanguageTag(LocalConfiguration.current)
    val result = remember(id, currentLanguageTag, *formatArgs) {
        val idName = context.resources.getResourceEntryName(id)
        try {
            return@remember getStringForContext(context = context, id = id, formatArgs = formatArgs)
        } catch (e: Exception) {
            Timber.e(e, "Unable to retrieve $idName for language $currentLanguageTag")
            try {
                val stringForEnglishContext = getStringForContext(
                    context = getEnglishContext(context),
                    id = id,
                    formatArgs = formatArgs
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

fun currentLanguageTag(configuration: Configuration): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        configuration.locales[0].toLanguageTag()
    } else {
        @Suppress("DEPRECATION")
        configuration.locale.toLanguageTag()
    }
}

private fun getStringForContext(
    context: Context,
    @StringRes id: Int,
    vararg formatArgs: Any,
): String {
    return context.resources.getString(id, *formatArgs)
}

fun getEnglishContext(context: Context): Context {
    var conf = context.resources.configuration
    conf = Configuration(conf)
    conf.setLocale(Locale("en"))
    val localizedContext = context.createConfigurationContext(conf)
    return localizedContext
}
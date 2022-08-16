package org.zotero.android.architecture

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

fun Int.prettyPrint(): String = String.format(Locale.getDefault(), "%,d", this)

data class SdkInt(val level: Int)

// Copied out from ui-components. Must be used only here. Using one from ui-components
// would require to add dependency to ui-components library. We should avoid that
// to streamline dependency graph and improve build time.

// NB: Also used in :home-data for color theme stuff, but that should go away
// due to deprecated code. Ideally we don't do theming at the data layer anymore
// with newer compose UI.
val Context.isDarkTheme: Boolean
    get() {
        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES
    }

package org.zotero.android.screens.htmlepub.settings.data

import androidx.annotation.StringRes
import org.zotero.android.uicomponents.Strings

enum class HtmlEpubSettingsOptions(@StringRes val optionStringId: Int) {
    AppearanceLight(Strings.pdf_settings_appearance_light_mode),
    AppearanceDark(Strings.pdf_settings_appearance_dark_mode),
    AppearanceAutomatic(Strings.pdf_settings_appearance_auto),
}
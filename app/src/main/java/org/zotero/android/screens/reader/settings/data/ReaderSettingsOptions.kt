package org.zotero.android.screens.reader.settings.data

import androidx.annotation.StringRes
import org.zotero.android.uicomponents.Strings

enum class ReaderSettingsOptions(@StringRes val optionStringId: Int) {
    AppearanceLight(Strings.pdf_settings_appearance_light_mode),
    AppearanceDark(Strings.pdf_settings_appearance_dark_mode),
    AppearanceAutomatic(Strings.pdf_settings_appearance_auto),

    ScrollDirectionHorizontal(Strings.pdf_settings_scroll_direction_horizontal),
    ScrollDirectionVertical(Strings.pdf_settings_scroll_direction_vertical),

    PageSpreadsNone(Strings.pdf_settings_page_mode_none),
    PageSpreadsDouble(Strings.pdf_settings_page_mode_double),
    PageSpreadsEven(Strings.pdf_settings_page_mode_even),

    PageLayoutFlowModePaginated(Strings.pdf_settings_flow_mode_paginated),
    PageLayoutFlowModeScrolled(Strings.pdf_settings_flow_mode_scrolled)
}
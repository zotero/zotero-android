package org.zotero.android.screens.reader.settings.data

import androidx.annotation.StringRes
import org.zotero.android.uicomponents.Strings

enum class ReaderSettingsOptions(@StringRes val optionStringId: Int) {
    AppearanceLight(Strings.pdf_settings_appearance_light_mode),
    AppearanceDark(Strings.pdf_settings_appearance_dark_mode),
    AppearanceAutomatic(Strings.pdf_settings_appearance_auto),

    ScrollModeVertical(Strings.pdf_settings_scroll_mode_vertical),
    ScrollModeHorizontal(Strings.pdf_settings_scroll_mode_horizontal),
    ScrollModeWrapped(Strings.pdf_settings_scroll_mode_wrapped),

    PageSpreadsNone(Strings.pdf_settings_page_mode_none),
    PageSpreadsDouble(Strings.pdf_settings_page_mode_double),
    PageSpreadsEven(Strings.pdf_settings_page_mode_even),

    PageLayoutFlowModePaginated(Strings.pdf_settings_flow_mode_paginated),
    PageLayoutFlowModeScrolled(Strings.pdf_settings_flow_mode_scrolled),

    FitToContentOff(Strings.pdf_settings_fit_to_content_off),
    FitToContentOn(Strings.pdf_settings_fit_to_content_on),

    ContentMarginNone(Strings.pdf_settings_content_margin_none),
    ContentMarginSmall(Strings.pdf_settings_content_margin_small),
    ContentMarginMedium(Strings.pdf_settings_content_margin_medium),
    ContentMarginLarge(Strings.pdf_settings_content_margin_large),
}
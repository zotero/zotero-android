package org.zotero.android.pdf.settings.data

import androidx.annotation.StringRes
import org.zotero.android.uicomponents.Strings

enum class PdfSettingsOptions(@StringRes val optionStringId: Int) {
    PageTransitionJump(Strings.pdf_settings_page_transition_jump),
    PageTransitionContinuous(Strings.pdf_settings_page_transition_continuous),

    PageModeSingle(Strings.pdf_settings_page_mode_single),
    PageModeDouble(Strings.pdf_settings_page_mode_double),
    PageModeAutomatic(Strings.pdf_settings_page_mode_automatic),

    ScrollDirectionHorizontal(Strings.pdf_settings_scroll_direction_horizontal),

    ScrollDirectionVertical(Strings.pdf_settings_scroll_direction_vertical),

    PageFittingFit(Strings.pdf_settings_page_fitting_fit),
    PageFittingFill(Strings.pdf_settings_page_fitting_fill),

    AppearanceLight(Strings.pdf_settings_appearance_light_mode),
    AppearanceDark(Strings.pdf_settings_appearance_dark_mode),
    AppearanceAutomatic(Strings.pdf_settings_appearance_auto),
}
package org.zotero.android.screens.htmlepub.settings.data

import org.zotero.android.pdf.data.PageAppearanceMode

data class HtmlEpubSettings(
    var appearanceMode: PageAppearanceMode,
) {
    companion object {
        fun default(): HtmlEpubSettings {
            return HtmlEpubSettings(
                appearanceMode = PageAppearanceMode.AUTOMATIC,
            )
        }
    }
}
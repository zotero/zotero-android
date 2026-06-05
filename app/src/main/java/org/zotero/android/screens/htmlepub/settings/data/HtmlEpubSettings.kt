package org.zotero.android.screens.htmlepub.settings.data

enum class PageAppearanceMode {
    LIGHT, DARK, AUTOMATIC
}

enum class PageScrollDirection {
    HORIZONTAL, VERTICAL
}

enum class PageSpreadsMode {
    SINGLE,
    DOUBLE,
    EVEN,
}

enum class PageLayoutFlowMode {
    PAGINATED,
    SCROLLED,
}


data class HtmlEpubSettings(
    var appearanceMode: PageAppearanceMode,
    var direction: PageScrollDirection,
    var spreadsMode: PageSpreadsMode,
    var pageLayoutFlowMode: PageLayoutFlowMode,
) {
    companion object {
        fun default(): HtmlEpubSettings {
            return HtmlEpubSettings(
                appearanceMode = PageAppearanceMode.AUTOMATIC,
                direction = PageScrollDirection.HORIZONTAL,
                spreadsMode = PageSpreadsMode.SINGLE,
                pageLayoutFlowMode = PageLayoutFlowMode.PAGINATED
            )
        }
    }
}
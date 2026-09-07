package org.zotero.android.screens.reader.settings.data

enum class PageAppearanceMode {
    LIGHT, DARK, AUTOMATIC
}

enum class PageScrollMode(val jsValue: Int) {
    VERTICAL(0),
    HORIZONTAL(1),
    WRAPPED(2),
}

enum class PageSpreadsMode {
    NONE,
    ODD,
    EVEN,
}

enum class PageLayoutFlowMode {
    PAGINATED,
    SCROLLED,
}


data class ReaderSettings(
    var appearanceMode: PageAppearanceMode,
    var scrollMode: PageScrollMode,
    var spreadsMode: PageSpreadsMode,
    var pageLayoutFlowMode: PageLayoutFlowMode,
) {
    companion object {
        fun default(): ReaderSettings {
            return ReaderSettings(
                appearanceMode = PageAppearanceMode.AUTOMATIC,
                scrollMode = PageScrollMode.VERTICAL,
                spreadsMode = PageSpreadsMode.NONE,
                pageLayoutFlowMode = PageLayoutFlowMode.PAGINATED
            )
        }
    }
}
package org.zotero.android.screens.reader.settings.data

enum class PageAppearanceMode {
    LIGHT, DARK, AUTOMATIC
}

enum class PageScrollDirection {
    HORIZONTAL, VERTICAL
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
    var direction: PageScrollDirection,
    var spreadsMode: PageSpreadsMode,
    var pageLayoutFlowMode: PageLayoutFlowMode,
) {
    companion object {
        fun default(): ReaderSettings {
            return ReaderSettings(
                appearanceMode = PageAppearanceMode.AUTOMATIC,
                direction = PageScrollDirection.HORIZONTAL,
                spreadsMode = PageSpreadsMode.NONE,
                pageLayoutFlowMode = PageLayoutFlowMode.PAGINATED
            )
        }
    }
}
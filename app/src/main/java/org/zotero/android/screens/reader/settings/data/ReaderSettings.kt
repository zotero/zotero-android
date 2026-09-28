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

// Space kept around fitted PDF content, as a fraction of the smaller viewport side
enum class PageContentMargin(val fraction: Double) {
    NONE(0.0),
    SMALL(0.02),
    MEDIUM(0.04),
    LARGE(0.08),
}


data class ReaderSettings(
    var appearanceMode: PageAppearanceMode,
    var scrollMode: PageScrollMode,
    var spreadsMode: PageSpreadsMode,
    var pageLayoutFlowMode: PageLayoutFlowMode,
    var fitToContent: Boolean = false,
    var contentMargin: PageContentMargin = PageContentMargin.SMALL,
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
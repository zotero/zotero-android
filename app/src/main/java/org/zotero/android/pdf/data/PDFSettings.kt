package org.zotero.android.pdf.data


enum class PageScrollMode {
 JUMP, CONTINUOUS
}

enum class PageLayoutMode {
    SINGLE,
    DOUBLE,
    AUTOMATIC,
}

enum class PageScrollDirection {
    HORIZONTAL, VERTICAL
}

enum class PageFitting {
    FIT, FILL
}

enum class PageAppearanceMode {
    LIGHT, DARK, AUTOMATIC
}


data class PDFSettings(
    var transition: PageScrollMode,
    var pageMode: PageLayoutMode,
    var direction: PageScrollDirection,
    var pageFitting: PageFitting,
    var appearanceMode: PageAppearanceMode,
    var idleTimerDisabled: Boolean,
) {
    companion object {
        fun default(): PDFSettings {
            return PDFSettings(
                transition = PageScrollMode.CONTINUOUS,
                pageMode = PageLayoutMode.AUTOMATIC,
                direction = PageScrollDirection.HORIZONTAL,
                pageFitting = PageFitting.FIT,
                appearanceMode = PageAppearanceMode.AUTOMATIC,
                idleTimerDisabled = false
            )
        }
    }
}
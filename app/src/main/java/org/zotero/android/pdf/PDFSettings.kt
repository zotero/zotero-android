package org.zotero.android.pdf

import com.pspdfkit.configuration.page.PageFitMode
import com.pspdfkit.configuration.page.PageLayoutMode
import com.pspdfkit.configuration.page.PageScrollDirection
import com.pspdfkit.configuration.page.PageScrollMode
import com.pspdfkit.configuration.theming.ThemeMode

data class PDFSettings(
    var transition: PageScrollMode,
    var pageMode: PageLayoutMode,
    var direction: PageScrollDirection,
    var pageFitting: PageFitMode,
    var appearanceMode: ThemeMode,
    var idleTimerDisabled: Boolean,
) {
    companion object {
        fun default(): PDFSettings {
            return PDFSettings(
                transition = PageScrollMode.CONTINUOUS,
                pageMode = PageLayoutMode.AUTO,
                direction = PageScrollDirection.HORIZONTAL,
                pageFitting = PageFitMode.FIT_TO_WIDTH,
                appearanceMode = ThemeMode.DEFAULT,
                idleTimerDisabled = false
            )
        }
    }
}
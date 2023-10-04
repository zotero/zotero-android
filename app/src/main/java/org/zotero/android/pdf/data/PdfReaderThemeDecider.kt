package org.zotero.android.pdf.data

import org.zotero.android.architecture.core.StateEventStream
import org.zotero.android.architecture.coroutines.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfReaderThemeDecider @Inject constructor(
    private val pdfReaderCurrentThemeEventStream: PdfReaderCurrentThemeEventStream
) {

    private var isOsThemeDark: Boolean = false
    private var pdfPageAppearanceMode: PageAppearanceMode = PageAppearanceMode.AUTOMATIC


    fun setCurrentOsTheme(isOsThemeDark: Boolean) {
        if (isOsThemeDark != this.isOsThemeDark) {
            this.isOsThemeDark = isOsThemeDark
            recalculateCurrentTheme()
        }
    }

    fun setPdfPageAppearanceMode(pageAppearanceMode: PageAppearanceMode) {
        if (pageAppearanceMode != this.pdfPageAppearanceMode) {
            this.pdfPageAppearanceMode = pageAppearanceMode
            recalculateCurrentTheme()
        }
    }

    private fun recalculateCurrentTheme() {
        val resultThemeIsDark =
        when (pdfPageAppearanceMode) {
            PageAppearanceMode.LIGHT -> false
            PageAppearanceMode.DARK -> true
            PageAppearanceMode.AUTOMATIC -> this.isOsThemeDark
        }

        pdfReaderCurrentThemeEventStream.emit(PdfReaderCurrentThemeData(resultThemeIsDark))

    }
}

@Singleton
class PdfReaderCurrentThemeEventStream @Inject constructor(applicationScope: ApplicationScope) :
    StateEventStream<PdfReaderCurrentThemeData>(applicationScope, PdfReaderCurrentThemeData(false))

data class PdfReaderCurrentThemeData(val isDark: Boolean)
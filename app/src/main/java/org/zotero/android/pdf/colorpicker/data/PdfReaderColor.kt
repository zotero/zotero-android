package org.zotero.android.pdf.colorpicker.data

data class PdfReaderColor(val colorName: String, val colorHex: String) {
    companion object {
        fun findByColorHex(colors: List<PdfReaderColor>, hex: String): PdfReaderColor? {
            return colors.firstOrNull { it.colorHex == hex }
        }
    }
}

package org.zotero.android.pdf.data

enum class PdfAnnotationChanges {
    color,
    boundingBox,
    rects,
    lineWidth,
    paths;

    companion object {
        fun stringValues(changes: List<PdfAnnotationChanges>): List<String> {
            val rawChanges = mutableListOf<String>()
            if (changes.contains(color)) {
                rawChanges.addAll(arrayOf("color", "alpha"))
            }
            if (changes.contains(rects)) {
                rawChanges.add("rects")
            }
            if (changes.contains(boundingBox)) {
                rawChanges.add("boundingBox")
            }
            if (changes.contains(lineWidth)) {
                rawChanges.add("lineWidth")
            }
            if (changes.contains(paths)) {
                rawChanges.addAll(arrayOf("lines", "lineArray"))
            }
            return rawChanges
        }
    }
}
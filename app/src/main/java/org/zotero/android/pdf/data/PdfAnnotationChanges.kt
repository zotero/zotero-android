package org.zotero.android.pdf.data

enum class PdfAnnotationChanges {
    color,
    boundingBox,
    rects,
    lineWidth,
    paths,
    contents,
    rotation,
    fontSize;

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
            if (changes.contains(contents)) {
                rawChanges.add("contents")
            }
            if (changes.contains(rotation)) {
                rawChanges.add("rotation")
            }
            if (changes.contains(fontSize)) {
                rawChanges.add("fontSize")
            }
            return rawChanges
        }
    }
}
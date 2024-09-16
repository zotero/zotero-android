package org.zotero.android.database.objects

enum class AnnotationType() {
    note,
    highlight,
    image,
    ink,
    underline,
    text;


    val kind: com.pspdfkit.annotations.AnnotationType
        get() {
            return when (this) {
                note -> com.pspdfkit.annotations.AnnotationType.NOTE
                highlight -> com.pspdfkit.annotations.AnnotationType.HIGHLIGHT
                image -> com.pspdfkit.annotations.AnnotationType.SQUARE
                ink -> com.pspdfkit.annotations.AnnotationType.INK
                underline -> com.pspdfkit.annotations.AnnotationType.UNDERLINE
                text -> com.pspdfkit.annotations.AnnotationType.FREETEXT
            }
        }
}



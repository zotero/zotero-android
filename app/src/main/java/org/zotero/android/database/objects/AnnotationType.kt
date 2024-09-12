package org.zotero.android.database.objects

enum class AnnotationType(val str: String) {
    note(""),
    highlight(""),
    image(""),
    ink(""),
    underline(""),
    freeText("text");


    val kind: com.pspdfkit.annotations.AnnotationType
        get() {
            return when (this) {
                note -> com.pspdfkit.annotations.AnnotationType.NOTE
                highlight -> com.pspdfkit.annotations.AnnotationType.HIGHLIGHT
                image -> com.pspdfkit.annotations.AnnotationType.SQUARE
                ink -> com.pspdfkit.annotations.AnnotationType.INK
                underline -> com.pspdfkit.annotations.AnnotationType.UNDERLINE
                freeText -> com.pspdfkit.annotations.AnnotationType.FREETEXT
            }
        }
}



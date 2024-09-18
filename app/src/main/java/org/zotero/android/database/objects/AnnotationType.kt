package org.zotero.android.database.objects

import com.pspdfkit.annotations.AnnotationType

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

fun AnnotationType.zoteroType(): org.zotero.android.database.objects.AnnotationType? {
    return when (this) {
        AnnotationType.HIGHLIGHT -> org.zotero.android.database.objects.AnnotationType.highlight
        AnnotationType.UNDERLINE -> org.zotero.android.database.objects.AnnotationType.underline
        AnnotationType.FREETEXT -> org.zotero.android.database.objects.AnnotationType.text
        AnnotationType.INK -> org.zotero.android.database.objects.AnnotationType.ink
        AnnotationType.SQUARE -> org.zotero.android.database.objects.AnnotationType.image
        AnnotationType.NOTE -> org.zotero.android.database.objects.AnnotationType.note
        else -> {
            null
        }
    }
}



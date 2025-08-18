package org.zotero.android.database.objects

import com.pspdfkit.annotations.AnnotationType

enum class AnnotationType() {
    note,
    highlight,
    image,
    ink,
    underline,
    text;


    val kind: AnnotationType
        get() {
            return when (this) {
                note -> AnnotationType.NOTE
                highlight -> AnnotationType.HIGHLIGHT
                image -> AnnotationType.SQUARE
                ink -> AnnotationType.INK
                underline -> AnnotationType.UNDERLINE
                text -> AnnotationType.FREETEXT
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



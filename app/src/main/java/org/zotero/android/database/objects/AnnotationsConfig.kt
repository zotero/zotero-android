package org.zotero.android.database.objects

import com.pspdfkit.annotations.AnnotationType
import java.util.EnumSet

object AnnotationsConfig {
    val positionSizeLimit = 65000
    val supported = EnumSet.of(
        AnnotationType.NOTE,
        AnnotationType.HIGHLIGHT,
        AnnotationType.SQUARE,
        AnnotationType.INK
    )
    val keyKey = "Zotero:Key"
    val noteAnnotationSize = Pair(22F, 22F)
}
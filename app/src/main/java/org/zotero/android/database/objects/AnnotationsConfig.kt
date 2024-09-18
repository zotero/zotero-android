package org.zotero.android.database.objects

import com.pspdfkit.annotations.AnnotationType
import org.zotero.android.sync.AnnotationColorGenerator
import java.util.EnumSet

object AnnotationsConfig {
    val defaultActiveColor = "#ffd400"
    val allColors = listOf(
        "#ffd400",
        "#ff6666",
        "#5fb236",
        "#2ea8e5",
        "#a28ae5",
        "#e56eee",
        "#f19837",
        "#aaaaaa",
        "#000000"
    )
    val typesWithColorVariation: List<org.zotero.android.database.objects.AnnotationType?> = listOf(
        null,
        org.zotero.android.database.objects.AnnotationType.highlight,
        org.zotero.android.database.objects.AnnotationType.underline
    )
    val userInterfaceStylesWithVarition: List<Boolean> = listOf(false, true)
    val positionSizeLimit = 65000
    val supported = EnumSet.of(
        AnnotationType.NOTE,
        AnnotationType.HIGHLIGHT,
        AnnotationType.SQUARE,
        AnnotationType.INK,
        AnnotationType.UNDERLINE,
        AnnotationType.FREETEXT,
    )
    val keyKey = "Zotero:Key"
    val noteAnnotationSize = Pair(22F, 22F)
    val imageAnnotationLineWidth = 2F
    val colorVariationMap: Map<Int, String> = createColorVariationMap()

    private fun createColorVariationMap(): Map<Int, String> {
        val map = mutableMapOf<Int, String>()
        for (hexBaseColor in this.allColors) {
            val baseColor = hexBaseColor
            for (type in typesWithColorVariation) {
                for (userInterfaceStyle in userInterfaceStylesWithVarition) {
                    val variation = AnnotationColorGenerator.color(
                        baseColor,
                        type = type,
                        isDarkMode = userInterfaceStyle
                    ).first
                    map[variation] = hexBaseColor
                }
            }
        }
        return map
    }

    fun colors(type: org.zotero.android.database.objects.AnnotationType): List<String> {
        return when (type) {
            org.zotero.android.database.objects.AnnotationType.ink, org.zotero.android.database.objects.AnnotationType.text -> {
                listOf(
                    "#ffd400",
                    "#ff6666",
                    "#5fb236",
                    "#2ea8e5",
                    "#a28ae5",
                    "#e56eee",
                    "#f19837",
                    "#aaaaaa",
                    "#000000"
                )
            }

            else -> {
                listOf(
                    "#ffd400",
                    "#ff6666",
                    "#5fb236",
                    "#2ea8e5",
                    "#a28ae5",
                    "#e56eee",
                    "#f19837",
                    "#aaaaaa"
                )
            }
        }
    }
}
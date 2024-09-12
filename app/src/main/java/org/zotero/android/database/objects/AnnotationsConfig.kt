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
            val color1 = AnnotationColorGenerator.color(
                colorHex = baseColor,
                isHighlight = false,
                isDarkMode = false
            ).first
            map[color1] = hexBaseColor
            val color2 = AnnotationColorGenerator.color(
                colorHex = baseColor,
                isHighlight = false,
                isDarkMode = true
            ).first
            map[color2] = hexBaseColor
            val color3 = AnnotationColorGenerator.color(
                colorHex = baseColor,
                isHighlight = true,
                isDarkMode = false
            ).first
            map[color3] = hexBaseColor
            val color4 = AnnotationColorGenerator.color(
                colorHex = baseColor,
                isHighlight = true,
                isDarkMode = true
            ).first
            map[color4] = hexBaseColor
        }
        return map
    }

    fun colors(type: org.zotero.android.database.objects.AnnotationType): List<String> {
        return when (type) {
            org.zotero.android.database.objects.AnnotationType.ink, org.zotero.android.database.objects.AnnotationType.freeText -> {
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
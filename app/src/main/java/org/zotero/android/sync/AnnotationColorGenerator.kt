package org.zotero.android.sync

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.pspdfkit.annotations.BlendMode
import org.zotero.android.database.objects.AnnotationType
import java.lang.Float.min


class AnnotationColorGenerator {
    companion object {
        private val highlightOpacity: Float = 0.5F
        private val highlightDarkOpacity: Float = 0.5F
        private val underlineOpacity: Float = 1F
        private val valunderlineDarkOpacity: Float = 1F


        fun color(
            colorHex: String,
            type: AnnotationType?,
            isDarkMode: Boolean
        ): Triple<Int, Float, BlendMode?> {
            val colorInt = android.graphics.Color.parseColor(colorHex)

            var opacity: Float = 1F

            when (type) {
                AnnotationType.note,
                AnnotationType.image,
                AnnotationType.ink,
                AnnotationType.text -> {
                    return Triple(colorInt, 1F, null)
                }

                AnnotationType.highlight -> {
                    if (isDarkMode) {
                        opacity = this.highlightDarkOpacity
                    } else {
                        opacity = this.highlightOpacity
                    }
                }

                AnnotationType.underline -> {
                    if (isDarkMode) {
                        opacity = this.valunderlineDarkOpacity
                    } else {
                        opacity = this.underlineOpacity
                    }
                }

                null -> {
                }
            }
            val adjustedColor: Int
            if (isDarkMode) {
                val hsv = getHSVFromColor(colorInt)
                val hue: Float = hsv[0]
                val sat: Float = hsv[1]
                val brg: Float = hsv[2]
//                var alpha: Float = android.graphics.Color.alpha(colorInt) / 255F

                val adjustedSat = min(1F, (sat * 1.2F))

                adjustedColor = android.graphics.Color.HSVToColor(
                    (opacity * 255).toInt(),
                    listOf(hue, adjustedSat, brg).toFloatArray()
                )

            } else {
                val color = Color(colorInt)
                adjustedColor = Color(
                    color.red,
                    color.green,
                    color.blue,
                    opacity
                ).toArgb()
            }
            return Triple(
                adjustedColor,
                opacity,
                blendMode(isDarkMode, type = type)
            )
        }

        fun blendMode(isDarkMode: Boolean, type: AnnotationType?): BlendMode? {
            when (type) {
                AnnotationType.note,
                AnnotationType.image,
                AnnotationType.ink,
                AnnotationType.text -> {
                    return null
                }

                AnnotationType.highlight,
                AnnotationType.underline -> {
                    return if (isDarkMode) BlendMode.LIGHTEN else BlendMode.MULTIPLY
                }

                null -> {
                    return null
                }
            }
        }

        private fun getHSVFromColor(color: Int): FloatArray {
            val hsv = FloatArray(3)
            android.graphics.Color.colorToHSV(color, hsv)
            return hsv
        }

    }

}
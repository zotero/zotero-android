package org.zotero.android.sync

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.pspdfkit.annotations.BlendMode
import java.lang.Float.min


class AnnotationColorGenerator {
    companion object {
        private val highlightOpacity: Float = 0.5F
        private val highlightDarkOpacity: Float = 0.5F

        fun color(
            colorHex: String,
            isHighlight: Boolean,
            isDarkMode: Boolean
        ): Triple<Int, Float, BlendMode?> {
            val colorInt = android.graphics.Color.parseColor(colorHex)
            if (!isHighlight) {
                return Triple(colorInt, 1F, null)
            }

            if (isDarkMode) {
                val hsv = getHSVFromColor(colorInt)
                var hue: Float = hsv[0]
                var sat: Float = hsv[1]
                var brg: Float = hsv[2]
                var alpha: Float = android.graphics.Color.alpha(colorInt) / 255F

                val adjustedSat = min(1F, (sat * 1.2F))

                val adjustedColor = android.graphics.Color.HSVToColor(
                    (AnnotationColorGenerator.highlightDarkOpacity * 255).toInt(),
                    listOf(hue, adjustedSat, brg).toFloatArray()
                )
                return Triple(
                    adjustedColor,
                    AnnotationColorGenerator.highlightDarkOpacity,
                    BlendMode.LIGHTEN
                )
            } else {
                val color = Color(colorInt)
                val adjustedColor = Color(
                    color.red,
                    color.green,
                    color.blue,
                    AnnotationColorGenerator.highlightOpacity
                )
                return Triple(
                    adjustedColor.toArgb(),
                    AnnotationColorGenerator.highlightOpacity,
                    BlendMode.MULTIPLY
                )
            }
        }

        fun blendMode(isDarkMode: Boolean, isHighlight: Boolean): BlendMode? {
            if (!isHighlight) {
                return null
            }
            return if (isDarkMode) BlendMode.LIGHTEN else BlendMode.MULTIPLY
        }

        fun getHSVFromColor(color: Int): FloatArray {
            val hsv = FloatArray(3)
            android.graphics.Color.colorToHSV(color, hsv)
            return hsv
        }

    }

}
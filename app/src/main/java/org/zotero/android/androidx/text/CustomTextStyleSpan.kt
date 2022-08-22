package org.zotero.android.androidx.text

import android.graphics.Typeface
import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.GenericFontFamily

class CustomTextStyleSpan(private val textStyle: TextStyle) : MetricAffectingSpan() {
    override fun updateDrawState(paint: TextPaint) {
        paint.textSize = textStyle.fontSize.value * paint.density
        paint.letterSpacing = textStyle.letterSpacing.value
        val font = (textStyle.fontFamily as? GenericFontFamily)?.name
        if (font != null) {
            paint.typeface = Typeface.create(font, textStyle.fontStyle?.value ?: 0)
        }
    }

    override fun updateMeasureState(paint: TextPaint) {
        paint.textSize = textStyle.fontSize.value * paint.density
        paint.letterSpacing = textStyle.letterSpacing.value
        val font = (textStyle.fontFamily as? GenericFontFamily)?.name
        if (font != null) {
            paint.typeface = Typeface.create(font, textStyle.fontStyle?.value ?: 0)
        }
    }
}

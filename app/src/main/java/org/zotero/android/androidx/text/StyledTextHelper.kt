package org.zotero.android.androidx.text

import android.content.Context
import android.graphics.Typeface
import android.text.Annotation
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.SpannedString
import android.text.style.AbsoluteSizeSpan
import android.text.style.CharacterStyle
import android.text.style.ForegroundColorSpan
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.getSpans
import androidx.core.text.toSpannable
import org.zotero.android.androidx.content.spToPx
import org.zotero.android.uicomponents.Fonts
import org.zotero.android.uicomponents.theme.CustomPalette
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.theme.CustomTypography
import timber.log.Timber

object StyledTextHelper {
    
    fun createStyledSpan(@StringRes stringRes: Int, context: Context, vararg args: Any): Spannable {
        val spannedString = context.getText(stringRes) as? SpannedString
            ?: return SpannableString(context.getString(stringRes))

        // get all the annotation spans from the text
        val annotations: Array<out Annotation> = spannedString.getSpans(0, spannedString.length)

        /**
         * Applies annotations with `font` key.
         */
        fun applyFont(annotation: Annotation): CharacterStyle {
            val typeface = getTypeface(context, annotation.value)
            return CustomTypefaceSpan(typeface)
        }

        /**
         * Applies annotations with `size` key.
         */
        fun applySize(annotation: Annotation): CharacterStyle {

            val size = try {
                annotation.value.toInt()
            } catch (e: NumberFormatException) {
                Timber.e("can't parse ${annotation.value}")
                0
            }

            return AbsoluteSizeSpan(context.spToPx(size))
        }

        /**
         * Applies annotations with `color` key.
         */
        fun applyColor(annotation: Annotation): CharacterStyle {

            val colorInt = when (val value = annotation.value) {
                "secondary_content" -> CustomPalette.CoolGray
                else -> error("Unknown color: $value!")
            }.toArgb()

            return ForegroundColorSpan(colorInt)
        }

        fun applyStyle(annotation: Annotation): CharacterStyle {
            val textStyle = getTextStyle(annotation.value)
            return CustomTextStyleSpan(textStyle)
        }

        // Replace arguments first
        val builder = annotations.fold(
            initial = SpannableStringBuilder(spannedString)
        ) { acc: SpannableStringBuilder, annotation: Annotation ->
            if (annotation.key == "arg") {
                acc.replace(
                    acc.getSpanStart(annotation),
                    acc.getSpanEnd(annotation),
                    String.format(annotation.value, *args)
                )
            } else {
                acc
            }
        }

        // iterate through all the styling annotation spans
        for (annotation in annotations) {
            val what = when (annotation.key) {
                "color" -> applyColor(annotation)
                "font" -> applyFont(annotation)
                "size" -> applySize(annotation)
                "style" -> applyStyle(annotation)
                else -> continue
            }

            // set the span at the same indices as the annotation
            builder.setSpan(
                what,
                builder.getSpanStart(annotation),
                builder.getSpanEnd(annotation),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        return builder.toSpannable()
    }

    @Composable
    fun annotatedStringResource(
        context: Context,
        @StringRes stringRes: Int,
        vararg args: Any
    ): AnnotatedString {
        val styledSpan = createStyledSpan(stringRes, context, *args)
        val annotations: Array<out Annotation> = styledSpan.getSpans()

        return buildAnnotatedString {
            append(styledSpan.toString())

            annotations.forEach { annotation ->
                val spanStyle = when (annotation.key) {
                    "color" -> applyColor(annotation)
                    "font" -> applyFont(context, annotation)
                    "size" -> applySize(annotation)
                    "style" -> applyStyle(annotation)
                    else -> return@forEach
                }
                addStyle(
                    style = spanStyle,
                    start = styledSpan.getSpanStart(annotation),
                    end = styledSpan.getSpanEnd(annotation)
                )
            }
        }
    }

    @Composable
    fun annotatedStringResource(
        @StringRes stringRes: Int,
        vararg args: Any
    ) = annotatedStringResource(
        context = LocalContext.current,
        stringRes = stringRes,
        args = args
    )

    private fun applyFont(
        context: Context,
        annotation: Annotation,
    ): SpanStyle {
        return SpanStyle(
            fontFamily = FontFamily(getTypeface(context, annotation.value))
        )
    }

    private fun applySize(
        annotation: Annotation,
    ): SpanStyle {
        return SpanStyle(
            fontSize = annotation.value.toInt().sp
        )
    }

    private fun applyStyle(
        annotation: Annotation,
    ): SpanStyle = getTextStyle(annotation.value).toSpanStyle()

    @Composable
    private fun applyColor(
        annotation: Annotation,
    ): SpanStyle {

        val color = when (val value = annotation.value) {
            "secondary_content" -> CustomTheme.colors.secondaryContent
            else -> error("Unknown color: $value!")
        }

        return SpanStyle(color = color)
    }

    private fun getTypeface(context: Context, font: String): Typeface {
        val resource = when (font) {
            "bold" -> return Typeface.DEFAULT_BOLD
            "reckless" -> Fonts.reckless_neue_book
            "grenette_semibold_italic" -> Fonts.grenette_semibold_italic_pro
            else -> error("Unknown font: $font!")
        }

        return ResourcesCompat.getFont(context, resource)!!
    }

    private fun getTextStyle(style: String): TextStyle {
        val typography = CustomTypography()
        return when (style) {
            "default" -> typography.default
            "defaultBold" -> typography.defaultBold
            "h1" -> typography.h1
            "h2" -> typography.h2
            "h3" -> typography.h3
            "h4" -> typography.h4
            "h5" -> typography.h5
            "h6" -> typography.h6
            "h7" -> typography.h7
            else -> error("Unknown style: $style!")
        }
    }
}

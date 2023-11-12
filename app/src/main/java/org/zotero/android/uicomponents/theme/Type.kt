package org.zotero.android.uicomponents.theme

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import org.zotero.android.uicomponents.Fonts

val LocalCustomTypography: ProvidableCompositionLocal<CustomTypography> =
    staticCompositionLocalOf { CustomTypography() }

private val fonts = FontFamily(
    Font(Fonts.suisse_intl_regular, weight = FontWeight.Normal),
    Font(Fonts.suisse_intl_regular_italic, weight = FontWeight.Normal, style = FontStyle.Italic),
    Font(Fonts.suisse_intl_medium, weight = FontWeight.Medium),
    Font(Fonts.suisse_intl_medium_italic, weight = FontWeight.Medium, style = FontStyle.Italic),
    Font(Fonts.suisse_intl_semibold, weight = FontWeight.SemiBold),
    Font(Fonts.suisse_intl_semibold_italic, weight = FontWeight.SemiBold, style = FontStyle.Italic),
    Font(Fonts.suisse_intl_bold, weight = FontWeight.Bold),
    Font(Fonts.suisse_intl_bold_italic, weight = FontWeight.Bold, style = FontStyle.Italic),
)

data class CustomTypography(
    val displayLarge: TextStyle = TextStyle(
        fontSize = 44.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
        lineHeight = 44.sp
    ),
    val displayMedium: TextStyle = TextStyle(
        fontSize = 40.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
        lineHeight = 40.sp,
    ),
    val displaySmall: TextStyle = TextStyle(
        fontSize = 32.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
        lineHeight = 32.sp,
    ),
    val subheadLight: TextStyle = TextStyle(
        fontSize = 28.sp,
        fontWeight = FontWeight.Light,
        letterSpacing = 0.sp,
        lineHeight = 32.sp,
        fontFamily = FontFamily(Font(Fonts.reckless_neue_book)),
    ),
    val h1: TextStyle = TextStyle(
        fontSize = 23.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = fonts,
        lineHeight = 27.6.sp,
        letterSpacing = 0.sp,
    ),
    val h1point5: TextStyle = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = fonts,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
    ),
    val h2: TextStyle = TextStyle(
        fontSize = 17.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = fonts,
        letterSpacing = 0.sp,
        lineHeight = 20.4.sp,
    ),
    val h2Menu: TextStyle = TextStyle(
        fontSize = 17.sp,
        fontWeight = FontWeight.Normal,
        fontFamily = fonts,
        letterSpacing = 0.sp,
        lineHeight = 20.4.sp,
    ),
    val h3: TextStyle = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = fonts,
        letterSpacing = 0.sp,
        lineHeight = 18.sp,
    ),
    val h4: TextStyle = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        fontFamily = fonts,
        letterSpacing = 0.sp,
        lineHeight = 18.sp,
    ),
    val default: TextStyle = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Normal,
        fontFamily = fonts,
        letterSpacing = 0.sp,
        lineHeight = 21.sp,
    ),
    val defaultBold: TextStyle = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = fonts,
        letterSpacing = 0.sp,
        lineHeight = 21.sp,
    ),
    val h5: TextStyle = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = fonts,
        letterSpacing = 0.sp,
        lineHeight = 16.8.sp,
    ),
    val h6: TextStyle = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = fonts,
        letterSpacing = 0.sp,
        lineHeight = 16.8.sp,
    ),
    val h7: TextStyle = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        fontFamily = fonts,
        letterSpacing = 0.sp,
        lineHeight = 16.8.sp,
    ),
    val info: TextStyle = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        fontFamily = fonts,
        letterSpacing = 0.sp,
        lineHeight = 20.sp,
    ),
    val caption: TextStyle = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = fonts,
        letterSpacing = 0.sp,
        lineHeight = 14.4.sp,
    ),
    val label: TextStyle = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Normal,
        fontFamily = fonts,
        letterSpacing = 0.sp,
        lineHeight = 12.sp,
    ),
    val labelBold: TextStyle = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = fonts,
        letterSpacing = 0.sp,
        lineHeight = 12.sp,
    ),
    val newTitleOne: TextStyle = TextStyle(
        fontSize = 28.sp,
        fontWeight = FontWeight.Normal,
        fontFamily = fonts,
        letterSpacing = 0.sp,
        lineHeight = 34.sp,
    ),

    val newHeadline: TextStyle = TextStyle(
        fontSize = 17.sp,
        fontWeight = FontWeight.Normal,//Supposed to be Semibold, but for suisse intl it's too 'strong'
        fontFamily = fonts,
        letterSpacing = 0.sp,
        lineHeight = 22.sp,
    ),
    val newBody: TextStyle = TextStyle(
        fontSize = 17.sp,
        fontWeight = FontWeight.Normal,
        fontFamily = fonts,
        letterSpacing = 0.sp,
        lineHeight = 22.sp,
    ),
    val newCaptionOne: TextStyle = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        fontFamily = fonts,
        letterSpacing = 0.sp,
        lineHeight = 16.sp,
    ),
)

@Preview(showBackground = true)
@Composable
fun TypographyPreview() {
    CustomTheme {
        Surface {
            Column {
                Text(
                    text = "Display Large",
                    style = CustomTheme.typography.displayLarge
                )
                Text(
                    text = "Display Medium",
                    style = CustomTheme.typography.displayMedium
                )
                Text(
                    text = "Display Small",
                    style = CustomTheme.typography.displaySmall
                )
                Text(
                    text = "Subhead Light",
                    style = CustomTheme.typography.subheadLight
                )
                Text(
                    text = "H1 Heading",
                    style = CustomTheme.typography.h1,
                )
                Text(
                    text = "H1.5 Heading",
                    style = CustomTheme.typography.h1point5,
                )
                Text(
                    text = "H2 Heading",
                    style = CustomTheme.typography.h2,
                )
                Text(
                    text = "H3 Heading",
                    style = CustomTheme.typography.h3,
                )
                Text(
                    text = "H4 Heading",
                    style = CustomTheme.typography.h4,
                )
                Text(
                    text = "Default",
                    style = CustomTheme.typography.default,
                )
                Text(
                    text = "H5 Heading",
                    style = CustomTheme.typography.h5,
                )
                Text(
                    text = "H6 Heading",
                    style = CustomTheme.typography.h6,
                )
                Text(
                    text = "H7 Heading",
                    style = CustomTheme.typography.h7,
                )
                Text(
                    text = "H8 Info",
                    style = CustomTheme.typography.info
                )
                Text(
                    text = "H9 Caption",
                    style = CustomTheme.typography.caption
                )
                Text(
                    text = "H10 Label",
                    style = CustomTheme.typography.label
                )
                Text(
                    text = "H10 LabelBold",
                    style = CustomTheme.typography.labelBold
                )
            }
        }
    }
}

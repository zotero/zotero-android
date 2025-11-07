package org.zotero.android.uicomponents.themem3

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val AppTypographyM3 = Typography(
    titleLarge = TextStyle(
        fontSize = 20.sp,
        letterSpacing = 0.sp,
        lineHeight = 28.sp,
    ),
    titleSmall = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.1.sp,
        lineHeight = 20.sp,
    ),
    titleSmallEmphasized = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.1.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        letterSpacing = 0.15.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        letterSpacing = 0.1.sp,
        lineHeight = 20.sp,
    ),
)
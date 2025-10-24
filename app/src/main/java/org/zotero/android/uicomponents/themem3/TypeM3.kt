package org.zotero.android.uicomponents.themem3

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val AppTypographyM3 = Typography(
	titleLarge = TextStyle(
        fontSize = 20.sp,
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
    ),
	titleSmallEmphasized = TextStyle(
		fontWeight = FontWeight.Bold,
	),
	bodyLarge = TextStyle(
        fontSize = 16.sp,
        letterSpacing = 0.15.sp,
    ),
	bodyMedium = TextStyle(
        letterSpacing = 0.1.sp,
    ),
)
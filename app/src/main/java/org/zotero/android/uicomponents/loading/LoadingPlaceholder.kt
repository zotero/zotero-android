package org.zotero.android.uicomponents.loading

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.placeholder.shimmer
import org.zotero.android.uicomponents.theme.CustomPalette
import org.zotero.android.uicomponents.theme.CustomTheme

/**
 * Generic placeholder element. Combine to create loading placeholders for
 * different screens.
 *
 * [isLeadingElement] Makes element more prominent on the screen.
 */
@Composable
fun LoadingPlaceholder(
    modifier: Modifier = Modifier,
    isLeadingElement: Boolean = false,
    shape: Shape,
) {
    Box(
        modifier = modifier
            .placeholder(
                visible = true,
                color = getBackgroundColor(isLeadingElement = isLeadingElement),
                shape = shape,
                highlight = getHighlight()
            )
    )
}

/**
 * Placeholder for a Text like UI element. Caller needs to specify
 * size through the modifier.
 *
 * [isLeadingElement] Makes element more prominent on the screen.
 */
@Composable
fun TextLoadingPlaceholder(
    modifier: Modifier = Modifier,
    isLeadingElement: Boolean = false
) {
    LoadingPlaceholder(
        modifier = modifier,
        isLeadingElement = isLeadingElement,
        shape = RoundedCornerShape(percent = 50),
    )
}

/**
 * Placeholder for a Icon UI element. Caller needs to specify
 * size through the modifier.
 */
@Composable
fun IconLoadingPlaceholder(modifier: Modifier = Modifier) {
    LoadingPlaceholder(
        modifier = modifier,
        isLeadingElement = false,
        shape = RectangleShape,
    )
}

@Composable
private fun getHighlight(): PlaceholderHighlight {
    return PlaceholderHighlight.shimmer(
        highlightColor = if (CustomTheme.colors.isLight) {
            Color(0xFFE6E8f2)
        } else {
            Color(0xFF363A45)
        },
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, delayMillis = 0),
            repeatMode = RepeatMode.Restart
        ),
        progressForMaxAlpha = 0.3f
    )
}

@Composable
private fun getBackgroundColor(isLeadingElement: Boolean): Color = when {
    CustomTheme.colors.isLight && isLeadingElement -> CustomPalette.FeatherGray
    CustomTheme.colors.isLight && !isLeadingElement -> CustomPalette.FogGray
    isLeadingElement -> CustomPalette.LightCharcoal
    else -> CustomPalette.Charcoal
}

@Preview(showBackground = true)
@Composable
private fun TextLoadingPlaceholderPreview() {
    CustomTheme {
        TextLoadingPlaceholder(
            modifier = Modifier
                .height(16.dp)
                .width(50.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun IconLoadingPlaceholderPreview() {
    CustomTheme {
        IconLoadingPlaceholder(modifier = Modifier.size(40.dp))
    }
}

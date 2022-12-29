package org.zotero.android.uicomponents.alignments

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.platform.InspectorValueInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.theme.CustomTheme
import kotlin.math.max
import kotlin.math.min

/**
 * [AlignmentLine] defined by the center of the first line of a Text. Use [placeCenterOfFirstAndLastLine]
 * to easily place this alignment line in your layout hierarchy.
 */
val CenterOfFirstLine = HorizontalAlignmentLine(::min)

/**
 * [AlignmentLine] defined by the center of the last line of a Text. Use [placeCenterOfFirstAndLastLine]
 * to easily place this alignment line in your layout hierarchy.
 */
val CenterOfLastLine = HorizontalAlignmentLine(::max)

/**
 * Places the [CenterOfFirstLine] and [CenterOfLastLine] alignment lines in the layout hierarchy.
 */
fun Modifier.placeCenterOfFirstAndLastLine(
    maxLines: Int = Int.MAX_VALUE,
): Modifier = then(
    PlaceCenterOfFirstAndLastLineLayoutModifier(
        maxLines = maxLines,
    )
)

private data class PlaceCenterOfFirstAndLastLineLayoutModifier(
    private val maxLines: Int,
) : LayoutModifier, InspectorValueInfo(
    debugInspectorInfo {
        name = "placeCenterOfFirstAndLastLineLayoutModifier"
        properties["maxLines"] = maxLines
    }
) {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val placeable = measurable.measure(constraints)

        val firstBaseLine = placeable[FirstBaseline]
            .let { if (it == AlignmentLine.Unspecified) 0 else it }
        val lastBaseLine = placeable[LastBaseline]
            .let { if (it == AlignmentLine.Unspecified) 0 else it }
        val height = placeable.measuredHeight

        val isSingleLine = firstBaseLine == lastBaseLine
        val isScrolling = !isSingleLine && (firstBaseLine < 0 || lastBaseLine > height)

        val singleLineHeight = when {
            isSingleLine -> {
                // When there is only one line, we can just use the height of the placeable.
                height
            }
            isScrolling -> {
                // When a constraint is applied through a max line or max character count FirstBaseline and
                // LastBaseline can report negative values depending on how much you've scrolled.
                // This typically happens when you've set maxLines for a TextField. We can just
                // divide by the passed in maxLines to get the height of a line in this case.
                height / maxLines
            }
            else -> {
                // In order to calculate the true height of a single line of text while still supporting multi
                // line text, we need to compute the difference between the baselines of the last line of text
                // and the first line of text. We subtract that from the total height of all lines to get the
                // value we're looking for.
                height - (lastBaseLine - firstBaseLine)
            }
        }
        val singleLineCenter = singleLineHeight / 2

        return layout(
            width = placeable.width,
            height = placeable.height,
            alignmentLines = mapOf(
                CenterOfFirstLine to singleLineCenter,
                CenterOfLastLine to placeable.height - singleLineCenter,
            )
        ) {
            placeable.place(0, 0)
        }
    }
}

@Preview(
    backgroundColor = 0xFFF,
    widthDp = 240,
    showBackground = true
)
@Composable
private fun CenterOfFirstLinePreview() {
    CustomTheme {
        Row {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(color = Color.Red)
                    .alignBy { it.measuredHeight / 2 }
            )

            Column(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .weight(1f)
                    .alignBy(CenterOfFirstLine)
            ) {
                Text(
                    text = "This is your very long title! ".repeat(2),
                    style = CustomTheme.typography.h4,
                    color = CustomTheme.colors.primaryContent,
                    modifier = Modifier
                        .placeCenterOfFirstAndLastLine()
                )

                Text(
                    text = "This is your long subtitle! ".repeat(2),
                    style = CustomTheme.typography.caption,
                    color = CustomTheme.colors.secondaryContent,
                    modifier = Modifier
                        .padding(top = 8.dp)
                )
            }
        }
    }
}

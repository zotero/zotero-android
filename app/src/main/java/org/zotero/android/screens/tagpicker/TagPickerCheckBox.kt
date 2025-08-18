package org.zotero.android.screens.tagpicker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun TagPickerCheckBox(
    isChecked: Boolean,
) {
    val checkboxSize = 24.dp
    if (isChecked) {
        val selectedBackgroundColor = CustomTheme.colors.zoteroDefaultBlue
        Box {
            Canvas(modifier = Modifier
                .size(checkboxSize)
                .align(Alignment.Center),
                onDraw = {
                    drawCircle(color = selectedBackgroundColor)
                }
            )
            Icon(
                modifier = Modifier.align(Alignment.Center),
                painter = painterResource(id = Drawables.check_small_24px),
                contentDescription = null,
                tint = Color.White
            )
        }
    } else {
        Canvas(modifier = Modifier
            .size(checkboxSize),
            onDraw = {
                drawCircle(
                    color = Color(0xFFC5C5C7),
                    radius = 12.dp.toPx(),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        )

    }
}
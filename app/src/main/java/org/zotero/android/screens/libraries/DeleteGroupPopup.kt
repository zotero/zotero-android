package org.zotero.android.screens.libraries

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.debounceClickable
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun DeleteGroupPopup(
    onDeleteGroup: () -> Unit,
    dismissDeleteGroupPopup: () -> Unit,
) {
    val backgroundColor = CustomTheme.colors.topBarBackgroundColor
    Popup(
        properties = PopupProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        ),
        onDismissRequest = dismissDeleteGroupPopup,
        popupPositionProvider = createDeleteGroupPopupPositionProvider(),
    ) {
        CustomScaffold(
            modifier = Modifier
                .width(250.dp)
                .height(48.dp)
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(16.dp),
                ),
            backgroundColor = backgroundColor,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .background(color = CustomTheme.colors.cardBackground)
                    .debounceClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true),
                        onClick = { onDeleteGroup() }
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    modifier = Modifier.weight(1f),
                    text = stringResource(id = Strings.remove),
                    maxLines = 1,
                    style = CustomTheme.typography.newBody,
                )
                Icon(
                    painter = painterResource(id = Drawables.delete_24px),
                    contentDescription = null,
                    tint = CustomTheme.colors.error
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}

@Composable
private fun createDeleteGroupPopupPositionProvider() = object : PopupPositionProvider {
    val localDensity = LocalDensity.current
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val extraXOffset = with(localDensity) {
            14.dp.toPx()
        }.toInt()
        val extraYOffset = with(localDensity) {
            6.dp.toPx()
        }.toInt()

        val xOffset = anchorBounds.left + extraXOffset
        val yOffset = anchorBounds.bottom + extraYOffset

        return IntOffset(
            x = xOffset,
            y = yOffset
        )
    }
}
package org.zotero.android.screens.settings.account

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.misc.PopupDivider
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.webdav.data.FileSyncType

@Composable
internal fun WebDavOptionsPopup(
    fileSyncType: FileSyncType,
    onZoteroOptionSelected: () -> Unit,
    onWebDavOptionSelected: () -> Unit,
    dismissWebDavOptionsPopup: () -> Unit,
) {
    Popup(
        properties = PopupProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        ),
        onDismissRequest = dismissWebDavOptionsPopup,
        popupPositionProvider = createWebDavOptionsPopupPositionProvider(),
    ) {
        Column(
            modifier = Modifier
                .width(250.dp)
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(16.dp),
                )
                .background(color = CustomTheme.colors.popupBackgroundColor)
        ) {
            if (fileSyncType == FileSyncType.zotero) {
                PopupOptionRow(
                    text = stringResource(id = Strings.file_syncing_zotero_option),
                    resIcon = Drawables.check_24px,
                    onOptionClick = onZoteroOptionSelected
                )
            } else {
                PopupOptionRow(
                    text = stringResource(id = Strings.file_syncing_zotero_option),
                    onOptionClick = onZoteroOptionSelected
                )
            }
            PopupDivider()
            if (fileSyncType == FileSyncType.webDav) {
                PopupOptionRow(
                    text = stringResource(id = Strings.file_syncing_webdav_option),
                    resIcon = Drawables.check_24px,
                    onOptionClick = onWebDavOptionSelected
                )
            } else {
                PopupOptionRow(
                    text = stringResource(id = Strings.file_syncing_webdav_option),
                    onOptionClick = onWebDavOptionSelected
                )
            }
        }
    }
}

@Composable
private fun createWebDavOptionsPopupPositionProvider() = object : PopupPositionProvider {
    val localDensity = LocalDensity.current
    val isTablet = CustomLayoutSize.calculateLayoutType().isTablet()
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val extraYOffset = with(localDensity) {
            12.dp.toPx()
        }.toInt()

        val q = windowSize.width - popupContentSize.width
        val xOffset = q - (windowSize.width - anchorBounds.right)

        val yOffset = anchorBounds.bottom + extraYOffset

        return IntOffset(
            x = xOffset,
            y = yOffset
        )
    }
}

@Composable
private fun PopupOptionRow(
    isEnabled: Boolean = true,
    textAndIconColor: Color? = null,
    text: String,
    @DrawableRes resIcon: Int? = null,
    onOptionClick: (() -> Unit)? = null,
) {
    val color = if (isEnabled) {
        textAndIconColor ?: CustomTheme.colors.primaryContent
    } else {
        Color(0xFF89898C)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth(1f)
            .heightIn(min = 44.dp)
            .background(color = CustomTheme.colors.popupRowBackgroundColor)
            .safeClickable(
                enabled = isEnabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(),
                onClick = onOptionClick,
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(8.dp))
        val iconSize = 24.dp
        if (resIcon != null) {
            Icon(
                modifier = Modifier.size(iconSize),
                painter = painterResource(resIcon),
                contentDescription = null,
                tint = color,
            )
        } else {
            Spacer(modifier = Modifier.width(iconSize))
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            modifier = Modifier.padding(vertical = 10.dp),
            color = color,
            text = text,
            style = CustomTheme.typography.newBody,
        )
        Spacer(modifier = Modifier.width(16.dp))
    }
}

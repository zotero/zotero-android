package org.zotero.android.screens.filter.popup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalDensity
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
import org.zotero.android.screens.filter.FilterViewModel
import org.zotero.android.screens.filter.FilterViewState
import org.zotero.android.screens.settings.elements.NewSettingsDivider
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Plurals
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.quantityStringResource

@Composable
internal fun FilterOptionsPopup(
    viewState: FilterViewState,
    viewModel: FilterViewModel,
) {
    Popup(
        properties = PopupProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        ),
        onDismissRequest = viewModel::dismissBottomSheet,
        popupPositionProvider = createFilterOptionsPopupPositionProvider(),
    ) {
        Column(
            modifier = Modifier
                .width(240.dp)
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(10.dp),
                )
                .background(color = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            val areDeselectAllRowsEnabled = viewState.selectedTags.isNotEmpty()
            FilterOptionsPopupOptionRow(
                isEnabled = false,
                text = quantityStringResource(
                    id = Plurals.tag_picker_tags_selected,
                    viewState.selectedTags.size
                ),
            )
            FilterOptionsPopupOptionRow(
                text = stringResource(id = Strings.items_deselect_all),
                isEnabled = areDeselectAllRowsEnabled,
                onOptionClick = viewModel::deselectAll
            )
            NewSettingsDivider()

            if (viewState.showAutomatic) {
                FilterOptionsPopupOptionRow(
                    text = stringResource(id = Strings.tag_picker_show_auto),
                    resIcon = Drawables.check_24px,
                    onOptionClick = { viewModel.setShowAutomatic(false) }
                )
            } else {
                FilterOptionsPopupOptionRow(
                    text = stringResource(id = Strings.tag_picker_show_auto),
                    onOptionClick = { viewModel.setShowAutomatic(true) }
                )
            }
            NewSettingsDivider()
            FilterOptionsPopupOptionRow(
                text = stringResource(id = Strings.tag_picker_delete_automatic),
                textAndIconColor = MaterialTheme.colorScheme.error,
                onOptionClick = viewModel::loadAutomaticCount
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun createFilterOptionsPopupPositionProvider() = object : PopupPositionProvider {
    val localDensity = LocalDensity.current
    val isTablet = CustomLayoutSize.calculateLayoutType().isTablet()
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val extraXOffset = with(localDensity) {
            16.dp.toPx()
        }.toInt()
        val extraYOffset = with(localDensity) {
            4.dp.toPx()
        }.toInt()

        val xOffset = if (isTablet) {
            anchorBounds.left + extraXOffset
        } else {
            val q = windowSize.width - popupContentSize.width
            q - (windowSize.width - anchorBounds.right)
        }

        val yOffset = if (isTablet) {
            anchorBounds.top - popupContentSize.height - extraYOffset
        } else {
            anchorBounds.bottom + extraYOffset
        }
        return IntOffset(
            x = xOffset,
            y = yOffset
        )
    }
}


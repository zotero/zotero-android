package org.zotero.android.screens.downloadedfiles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
import org.zotero.android.screens.allitems.AllItemsViewModel
import org.zotero.android.screens.allitems.AllItemsViewState
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.controls.CustomSwitch
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun DownloadedFilesPopup(
    viewState: AllItemsViewState,
    viewModel: AllItemsViewModel,
) {
    val backgroundColor = CustomTheme.colors.topBarBackgroundColor
    Popup(
        properties = PopupProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        ),
        onDismissRequest = viewModel::dismissDownloadedFilesPopup,
        popupPositionProvider = createDownloadedFilesPopupPositionProvider(),
    ) {
        CustomScaffold(
            modifier = Modifier
                .width(350.dp)
                .height(100.dp)
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(16.dp),
                ),
            backgroundColor = backgroundColor,
            topBar = {
                DownloadedFilesTopBar(
                    onDone = viewModel::dismissDownloadedFilesPopup,
                )
            },
        ) {
            Column(
                modifier = Modifier
                    .background(color = CustomTheme.colors.cardBackground)
            ) {
                Row(
                    modifier = Modifier.padding(top = 4.dp, start = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = stringResource(id = Strings.items_filters_downloads),
                        maxLines = 1,
                        style = CustomTheme.typography.newBody,
                    )
                    CustomSwitch(
                        checked = viewState.isDownloadsFilterEnabled(),
                        onCheckedChange = { viewModel.onDownloadedFilesTapped() },
                        modifier = Modifier
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }

        }
    }
}

@Composable
private fun createDownloadedFilesPopupPositionProvider() = object : PopupPositionProvider {
    val localDensity = LocalDensity.current
    val isTablet = CustomLayoutSize.calculateLayoutType().isTablet()
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val extraXOffset = with(localDensity) {
            -14.dp.toPx()
        }.toInt()
        val extraYOffset = with(localDensity) {
            2.dp.toPx()
        }.toInt()

        val xOffset = if (isTablet) {
            anchorBounds.left + (anchorBounds.right - anchorBounds.left) - popupContentSize.width / 2 + extraXOffset
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
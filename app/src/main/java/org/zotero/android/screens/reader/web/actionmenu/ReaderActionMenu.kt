package org.zotero.android.screens.reader.web.actionmenu

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AppBarMenuState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.zotero.android.screens.reader.ReaderViewModel
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun ReaderActionMenu(
    viewModel: ReaderViewModel,
) {
    val actions = generateActionMenuItems(
        viewModel = viewModel,
    )
    val panelItems = actions.panelItems
    var overflowItems = actions.overflowItems

    val maxItemsInPanel = 3
    val paneItemsToDisplay = panelItems.take(maxItemsInPanel)
    val extraOverflowItems =
        if (panelItems.size > maxItemsInPanel) {
            panelItems.subList(maxItemsInPanel, panelItems.size)
        } else {
            emptyList()
        }

    overflowItems = extraOverflowItems + overflowItems

    val menuState = remember { AppBarMenuState() }

    if (menuState.isExpanded) {
        ColumnActionMenu(
            menuState = menuState,
            overflowItems = overflowItems
        )
    } else {
        RowActionMenu(
            menuState = menuState,
            paneItemsToDisplay = paneItemsToDisplay,
            overflowItems = overflowItems
        )
    }
}

@Composable
private fun ColumnActionMenu(
    menuState: AppBarMenuState,
    overflowItems: List<ReaderActionMenuItem>,
) {
    val roundCornerShape = RoundedCornerShape(size = 20.dp)

    Column(
        modifier = Modifier
            .width(IntrinsicSize.Min)
            .clip(roundCornerShape)
            .background(CustomTheme.colors.windowBackground),
        content = {
            ReaderActionCollapseOverflowItem(onClick = { menuState.dismiss() })
            overflowItems
                .forEach { item ->
                    ReaderActionMenuContentItem(
                        onClick = item.onClick,
                        overflowTextResId = item.overflowTextResId,
                    )
                }
        })

}

@Composable
private fun RowActionMenu(
    menuState: AppBarMenuState,
    paneItemsToDisplay: List<ReaderActionMenuItem>,
    overflowItems: List<ReaderActionMenuItem>,
) {
    val roundCornerShape = RoundedCornerShape(size = 20.dp)
    Row(
        modifier = Modifier
            .width(IntrinsicSize.Min)
            .clip(roundCornerShape)
            .background(CustomTheme.colors.windowBackground),
        content = {
            Spacer(modifier = Modifier.width(8.dp))
            paneItemsToDisplay.forEach {
                ReaderActionMenuAppbarContentItem(
                    overflowTextResId = it.overflowTextResId,
                    onClick = it.onClick,
                )
            }
            if (overflowItems.isNotEmpty()) {
                IconButton(
                    onClick = {
                        menuState.show()
                    }
                ) {
                    Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "Overflow")
                }
            }
        })
}

@Composable
private fun generateActionMenuItems(
    viewModel: ReaderViewModel,
): ReaderActionMenuItems {
    val localActivity = LocalActivity.current

    val panelItems = mutableListOf<ReaderActionMenuItem>()
    val overflowItems = mutableListOf<ReaderActionMenuItem>()

    panelItems.add(
        ReaderActionMenuItem(
            overflowTextResId = Strings.copy_1,
            onClick = { viewModel.onCopy() })
    )

    panelItems.add(
        ReaderActionMenuItem(
            overflowTextResId = Strings.pdf_highlight,
            onClick = { viewModel.onHighlight() })
    )

    panelItems.add(
        ReaderActionMenuItem(
            overflowTextResId = Strings.pdf_underline,
            onClick = { viewModel.onUnderline() })
    )

    panelItems.add(
        ReaderActionMenuItem(
            overflowTextResId = Strings.translate,
            onClick = { viewModel.onTranslate(localActivity) })
    )
    panelItems.add(
        ReaderActionMenuItem(
            overflowTextResId = Strings.share,
            onClick = { viewModel.onShare(localActivity) })
    )

    panelItems.add(
        ReaderActionMenuItem(
            overflowTextResId = Strings.web_search,
            onClick = { viewModel.onWebSearch(localActivity) })
    )
    return ReaderActionMenuItems(panelItems = panelItems, overflowItems = overflowItems)
}

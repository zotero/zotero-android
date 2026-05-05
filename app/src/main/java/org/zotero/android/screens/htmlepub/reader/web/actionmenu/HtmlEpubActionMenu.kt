package org.zotero.android.screens.htmlepub.reader.web.actionmenu

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
import org.zotero.android.screens.htmlepub.reader.HtmlEpubReaderViewModel
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun HtmlEpubActionMenu(
    viewModel: HtmlEpubReaderViewModel,
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
    overflowItems: List<HtmlEpubActionMenuItem>,
) {
    val roundCornerShape = RoundedCornerShape(size = 20.dp)

    Column(
        modifier = Modifier
            .width(IntrinsicSize.Min)
            .clip(roundCornerShape)
            .background(CustomTheme.colors.windowBackground),
        content = {
            HtmlEpubActionCollapseOverflowItem(onClick = { menuState.dismiss() })
            overflowItems
                .forEach { item ->
                    HtmlEpubActionMenuContentItem(
                        onClick = item.onClick,
                        overflowTextResId = item.overflowTextResId,
                    )
                }
        })

}

@Composable
private fun RowActionMenu(
    menuState: AppBarMenuState,
    paneItemsToDisplay: List<HtmlEpubActionMenuItem>,
    overflowItems: List<HtmlEpubActionMenuItem>,
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
                HtmlEpubActionMenuAppbarContentItem(
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
    viewModel: HtmlEpubReaderViewModel,
): HtmlEpubActionMenuItems {
    val localActivity = LocalActivity.current

    val panelItems = mutableListOf<HtmlEpubActionMenuItem>()
    val overflowItems = mutableListOf<HtmlEpubActionMenuItem>()

    panelItems.add(
        HtmlEpubActionMenuItem(
            overflowTextResId = Strings.copy_1,
            onClick = { viewModel.onCopy() })
    )

    panelItems.add(
        HtmlEpubActionMenuItem(
            overflowTextResId = Strings.pdf_highlight,
            onClick = { viewModel.onHighlight() })
    )

    panelItems.add(
        HtmlEpubActionMenuItem(
            overflowTextResId = Strings.pdf_underline,
            onClick = { viewModel.onUnderline() })
    )

    panelItems.add(
        HtmlEpubActionMenuItem(
            overflowTextResId = Strings.translate,
            onClick = { viewModel.onTranslate(localActivity) })
    )
    panelItems.add(
        HtmlEpubActionMenuItem(
            overflowTextResId = Strings.share,
            onClick = { viewModel.onShare(localActivity) })
    )

    panelItems.add(
        HtmlEpubActionMenuItem(
            overflowTextResId = Strings.web_search,
            onClick = { viewModel.onWebSearch(localActivity) })
    )
    return HtmlEpubActionMenuItems(panelItems = panelItems, overflowItems = overflowItems)
}

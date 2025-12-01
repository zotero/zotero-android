package org.zotero.android.pdf.reader.sidebar

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.pdf.reader.PdfReaderVMInterface
import org.zotero.android.pdf.reader.PdfReaderViewState
import org.zotero.android.pdf.reader.PdfSidebarSearchBar
import org.zotero.android.pdf.reader.sidebar.data.Outline
import org.zotero.android.pdf.reader.sidebar.data.PdfReaderOutlineOptionsWithChildren
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.debounceCombinedClickable
import org.zotero.android.uicomponents.icon.IconWithPadding
import org.zotero.android.uicomponents.misc.NewDivider
import org.zotero.android.uicomponents.theme.CustomPalette
import org.zotero.android.uicomponents.theme.CustomTheme

private val levelPaddingConst = 8.dp

@Composable
internal fun PdfReaderOutlineSidebar(
    vMInterface: PdfReaderVMInterface,
    viewState: PdfReaderViewState,
    layoutType: CustomLayoutSize.LayoutType,
) {
    if (viewState.isOutlineEmpty) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Text(
                modifier = Modifier.align(Alignment.Center),
                text = stringResource(id = Strings.pdf_sidebar_no_outline),
                color = CustomPalette.SystemGray,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            )
        }

    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                )

        ) {
            Spacer(modifier = Modifier.height(16.dp))
            PdfSidebarSearchBar(
                searchValue = viewState.outlineSearchTerm,
                onSearch = vMInterface::onOutlineSearch,
            )
            Spacer(modifier = Modifier.height(8.dp))
            PdfReaderOutlineTable(
                vMInterface = vMInterface,
                viewState = viewState,
                layoutType = layoutType
            )
        }
    }
}

@Composable
internal fun PdfReaderOutlineTable(
    vMInterface: PdfReaderVMInterface,
    viewState: PdfReaderViewState,
    layoutType: CustomLayoutSize.LayoutType
) {
    val roundCornerShape = RoundedCornerShape(size = 10.dp)
    LazyColumn(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = roundCornerShape)
            .clip(roundCornerShape),
        state = rememberLazyListState(),
    ) {
        recursiveOutlineItem(
            layoutType = layoutType,
            outlineItems = viewState.outlineSnapshot,
            isCollapsed = { viewState.isOutlineSectionCollapsed(it.outline.id) },
            onItemTapped = { vMInterface.onOutlineItemTapped(it.outline) },
            onItemChevronTapped = { vMInterface.onOutlineItemChevronTapped(it.outline) },
        )
        item {
            Spacer(modifier = Modifier.windowInsetsPadding(NavigationBarDefaults.windowInsets))
        }
    }

}

private fun LazyListScope.recursiveOutlineItem(
    layoutType: CustomLayoutSize.LayoutType,
    levelPadding: Dp = 8.dp,
    outlineItems: List<PdfReaderOutlineOptionsWithChildren>,
    isCollapsed: (item: PdfReaderOutlineOptionsWithChildren) -> Boolean,
    onItemTapped: (item: PdfReaderOutlineOptionsWithChildren) -> Unit,
    onItemChevronTapped: (item: PdfReaderOutlineOptionsWithChildren) -> Unit,
) {
    for (item in outlineItems) {
        item {
            OutlineItem(
                levelPadding = levelPadding,
                outline = item.outline,
                hasChildren = item.children.isNotEmpty(),
                isCollapsed = isCollapsed(item),
                onItemTapped = { onItemTapped(item) },
                onItemChevronTapped = { onItemChevronTapped(item) }
            )
        }

        if (!isCollapsed(item)) {
            recursiveOutlineItem(
                layoutType = layoutType,
                levelPadding = levelPadding + levelPaddingConst,
                outlineItems = item.children,
                isCollapsed = isCollapsed,
                onItemTapped = onItemTapped,
                onItemChevronTapped = onItemChevronTapped
            )
        }
    }
}

@Composable
private fun OutlineItem(
    levelPadding: Dp,
    outline: Outline,
    hasChildren: Boolean,
    isCollapsed: Boolean,
    onItemTapped: () -> Unit,
    onItemChevronTapped: () -> Unit,
) {
    val rowModifier = Modifier.heightIn(min = 44.dp)
    val arrowIconAreaSize = 32.dp
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = rowModifier
                .debounceCombinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(),
                    onClick = onItemTapped,
                )
        ) {
            Spacer(modifier = Modifier.width(levelPadding))
            Text(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp),
                text = outline.title,
                style = CustomTheme.typography.newBody,
                color = if (outline.isActive) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    CustomPalette.SystemGray
                },
            )
            Spacer(modifier = Modifier.width(8.dp))
            if (hasChildren) {
                IconWithPadding(
                    drawableRes = if (isCollapsed) {
                        Drawables.chevron_right_24px
                    } else {
                        Drawables.expand_more_24px
                    },
                    onClick = { onItemChevronTapped() },
                    areaSize = arrowIconAreaSize,
                    tintColor = MaterialTheme.colorScheme.primary,
                    shouldShowRipple = false
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
        NewDivider(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = levelPadding)
        )
    }
}

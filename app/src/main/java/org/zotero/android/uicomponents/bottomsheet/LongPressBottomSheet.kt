package org.zotero.android.uicomponents.bottomsheet

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.uicomponents.misc.CustomDivider
import org.zotero.android.uicomponents.modal.CustomModalBottomSheet
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun LongPressBottomSheet(
    onCollapse: () -> Unit,
    layoutType: CustomLayoutSize.LayoutType,
    longPressOptionsHolder: LongPressOptionsHolder? = null,
    onOptionClick: (LongPressOptionItem) -> Unit,
    tabletWidthPercentage: Float = 0.5f,
) {
    val shouldShow = longPressOptionsHolder != null

    if (shouldShow) {
        CustomModalBottomSheet(
            sheetContent = {
                LongPressBottomSheetContent(
                    layoutType = layoutType,
                    longPressOptionsHolder = longPressOptionsHolder!!,
                    tabletWidthPercentage = tabletWidthPercentage,
                    onOptionClick = {
                        onCollapse()
                        onOptionClick(it)
                    })
            },
            onCollapse = {
                onCollapse()
            },
        )
    }

}

@Composable
private fun BoxScope.LongPressBottomSheetContent(
    layoutType: CustomLayoutSize.LayoutType,
    longPressOptionsHolder: LongPressOptionsHolder,
    onOptionClick: (LongPressOptionItem) -> Unit,
    tabletWidthPercentage: Float
) {
    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth(if (layoutType.isTablet()) tabletWidthPercentage else 1f),
    ) {
        Text(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(
                    start = 16.dp, end = 16.dp, top = 16.dp
                ),
            text = longPressOptionsHolder.title,
            color = if (longPressOptionsHolder.isTitleEnabled) {
                CustomTheme.colors.primaryContent
            } else {
                CustomTheme.colors.disabledContent
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = CustomTheme.typography.newBody,
        )
        longPressOptionsHolder.longPressOptionItems.forEach { mention ->
            LongPressOptionRow(
                optionItem = mention,
                onOptionClick = onOptionClick
            )
            CustomDivider()
        }
    }
}

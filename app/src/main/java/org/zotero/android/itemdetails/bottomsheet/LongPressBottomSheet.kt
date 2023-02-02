package org.zotero.android.itemdetails.bottomsheet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
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
) {
    val shouldShow = longPressOptionsHolder != null

    if (shouldShow) {
        CustomModalBottomSheet(
            sheetContent = {
                LongPressBottomSheetContent(
                    layoutType = layoutType,
                    longPressOptionsHolder = longPressOptionsHolder!!,
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
private fun LongPressBottomSheetContent(
    layoutType: CustomLayoutSize.LayoutType,
    longPressOptionsHolder: LongPressOptionsHolder,
    onOptionClick: (LongPressOptionItem) -> Unit,
) {
        Column(
        ) {
            Text(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(
                        start = 16.dp, end = 16.dp, top = 16.dp
                    ),
                text = longPressOptionsHolder.title,
                color = CustomTheme.colors.primaryContent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = CustomTheme.typography.default,
                fontSize = layoutType.calculateTextSize(),
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

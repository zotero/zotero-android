package org.zotero.android.uicomponents.bottomsheet

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.zotero.android.screens.settings.elements.NewSettingsDivider
import org.zotero.android.uicomponents.modal.CustomModalBottomSheetM3
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun LongPressBottomSheetM3(
    onCollapse: () -> Unit,
    longPressOptionsHolder: LongPressOptionsHolder? = null,
    onOptionClick: (LongPressOptionItem) -> Unit,
) {
    val shouldShow = longPressOptionsHolder != null

    if (shouldShow) {
        CustomModalBottomSheetM3(
            sheetContent = {
                LongPressBottomSheetContent(
                    longPressOptionsHolder = longPressOptionsHolder,
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
    longPressOptionsHolder: LongPressOptionsHolder,
    onOptionClick: (LongPressOptionItem) -> Unit,
) {
    Column(
        modifier = Modifier
            .windowInsetsPadding(BottomAppBarDefaults.windowInsets)
            .align(Alignment.BottomCenter)
            .fillMaxWidth(1f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp)
        ) {
            Text(
                modifier = Modifier
                    .align(Alignment.CenterVertically),
                text = longPressOptionsHolder.title,
                color = if (longPressOptionsHolder.isTitleEnabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    CustomTheme.colors.disabledContent
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        NewSettingsDivider()
        longPressOptionsHolder.longPressOptionItems.forEach { mention ->
            LongPressOptionRowM3(
                optionItem = mention,
                onOptionClick = onOptionClick
            )
        }
    }
}

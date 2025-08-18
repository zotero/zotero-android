package org.zotero.android.screens.tagpicker

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.misc.CustomDivider
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun CreateTagRow(
    tagName: String,
    onClick: () -> Unit,
    layoutType: CustomLayoutSize.LayoutType
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .safeClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(start = 16.dp),
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(id = Strings.tag_picker_create_tag, tagName),
            fontSize = layoutType.calculateTextSize(),
            color = CustomTheme.colors.primaryContent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(8.dp))
        CustomDivider()
    }

}

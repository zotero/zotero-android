package org.zotero.android.screens.sortpicker

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.misc.NewDivider
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun SortPickerFieldTappableRow(
    detailTitle: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .height(60.dp)
            .safeClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick
            )
    ) {
        Row(modifier = Modifier.align(Alignment.CenterStart)) {
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = detailTitle,
                style = CustomTheme.typography.newBody,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = CustomTheme.colors.primaryContent,
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                painter = painterResource(id = Drawables.chevron_right_24px),
                contentDescription = null,
                tint = CustomTheme.colors.chevronNavigationColor
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
        NewDivider(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp)
        )
    }
}
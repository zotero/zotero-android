package org.zotero.android.uicomponents.badge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.zotero.android.architecture.prettyPrint
import org.zotero.android.uicomponents.theme.CustomTheme


@Composable
fun RoundBadgeIcon(
    count: Int,
) {
    Text(
        text = count.prettyPrint(),
        style = CustomTheme.typography.newCaptionOne,
        color = CustomTheme.colors.primaryContent,
        modifier = Modifier
            .height(18.dp)
            .background(
                color = CustomTheme.colors.zoteroItemDetailSectionBackground,
                shape = RoundedCornerShape(size = 100.dp)
            )
            .padding(horizontal = 8.dp, vertical = 1.dp),
        textAlign = TextAlign.Center,
    )

}

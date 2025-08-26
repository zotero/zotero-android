package org.zotero.android.screens.allitems.table.rows

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.zotero.android.screens.allitems.data.ItemCellModel
import org.zotero.android.uicomponents.Drawables

@Composable
internal fun RowScope.ItemRowTitleAndSubtitlePart(model: ItemCellModel) {
    Column(
        modifier = Modifier.weight(1f)
    ) {
        Text(
            text = model.title.ifEmpty { " " },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row {
            var subtitleText = model.subtitle.ifEmpty { " " }
            val shouldHideSubtitle =
                model.subtitle.isEmpty() && (model.hasNote || !model.tagColors.isEmpty())
            if (shouldHideSubtitle) {
                subtitleText = ""
            }
            Text(
                text = subtitleText,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (model.hasNote) {
                if (model.subtitle.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Image(
                    modifier = Modifier
                        .size(12.dp)
                        .align(Alignment.CenterVertically),
                    painter = painterResource(id = Drawables.cell_note),
                    contentDescription = null,
                )
            }
        }
    }
}
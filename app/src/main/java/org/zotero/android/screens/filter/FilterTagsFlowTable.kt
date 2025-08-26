package org.zotero.android.screens.filter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import org.zotero.android.uicomponents.foundation.safeClickable

internal fun LazyListScope.filterTagsFlowTable(
    viewState: FilterViewState,
    viewModel: FilterViewModel
) {
    items(items = viewState.tags) { chunkedList ->
        FlowRow(
            modifier = Modifier,
        ) {
            chunkedList.forEach {
                val roundCornerShape = RoundedCornerShape(8.dp)
                var rowModifier: Modifier = Modifier
                    .padding(horizontal = 4.dp, vertical = 4.dp)
                    .clip(shape = roundCornerShape)
                val selected = viewState.selectedTags.contains(it.tag.name)
                if (selected) {
                    val backgroundColor = MaterialTheme.colorScheme.secondaryContainer
                    rowModifier = rowModifier
                        .background(backgroundColor)
                        .border(
                            width = 1.dp,
                            color = backgroundColor,
                            shape = roundCornerShape
                        )
                }
                Box(
                    modifier = rowModifier
                        .safeClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { viewModel.onTagTapped(it) },
                        )
                ) {
                    val textColor = if (it.tag.color.isNotEmpty()) {
                        val color = it.tag.color.toColorInt()
                        Color(color)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                    Text(
                        modifier = Modifier.padding(
                            vertical = 3.dp,
                            horizontal = 14.dp
                        ),
                        text = it.tag.name,
                        color = if (it.isActive) textColor else textColor.copy(alpha = 0.55f),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
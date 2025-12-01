package org.zotero.android.screens.addnote

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.safeClickable

@Composable
internal fun BoxScope.AddNoteTagSelector(
    viewState: AddNoteViewState,
    viewModel: AddNoteViewModel,
) {
    Row(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .windowInsetsPadding(NavigationBarDefaults.windowInsets)
            .height(48.dp)
            .background(color = MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp)
            .safeClickable(
                onClick = viewModel::onTagsClicked,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(id = Strings.items_filters_tags) + ":",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )

        val formattedTags = viewState.formattedTags()
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp),
            text = formattedTags,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

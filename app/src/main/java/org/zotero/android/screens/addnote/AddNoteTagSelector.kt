package org.zotero.android.screens.addnote

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.theme.CustomPalette
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun BoxScope.AddNoteTagSelector(
    viewState: AddNoteViewState,
    viewModel: AddNoteViewModel,
    layoutType: CustomLayoutSize.LayoutType
) {
    Row(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .background(color = CustomTheme.colors.zoteroItemDetailSectionBackground)
            .padding(horizontal = 16.dp, vertical = 20.dp)
            .safeClickable(
                onClick = viewModel::onTagsClicked,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(id = Strings.items_filters_tags) + ":",
            color = CustomPalette.zoteroItemDetailSectionTitle,
            fontSize = layoutType.calculateTextSize(),
        )

        val formattedTags = viewState.formattedTags()
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp),
            text = formattedTags,
            color = CustomTheme.colors.primaryContent,
            style = CustomTheme.typography.default,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = layoutType.calculateTextSize(),
        )

        Icon(
            painter = painterResource(Drawables.ic_arrow_small_right),
            contentDescription = null,
            modifier = Modifier.padding(start = 8.dp),
            tint = CustomTheme.colors.zoteroDefaultBlue,
        )
    }
}

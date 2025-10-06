package org.zotero.android.screens.creatoredit.rows

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.zotero.android.screens.creatoredit.CreatorEditViewModel
import org.zotero.android.screens.creatoredit.CreatorEditViewState
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.safeClickable

@Composable
internal fun CreatorEditDeleteCreatorRow(
    viewModel: CreatorEditViewModel,
    viewState: CreatorEditViewState,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .safeClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = viewModel::showDeleteCreatorConfirmation
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier
                .padding(start = 16.dp),
            text = stringResource(
                id = Strings.delete
            ) + " " + viewState.creator?.localizedType,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
package org.zotero.android.screens.collectionedit

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun CollectionEditTopBar(
    onCancel: () -> Unit,
    onSave: () -> Unit,
    viewState: CollectionEditViewState
) {

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        title = {
            Text(
                text = stringResource(
                    id = if (viewState.key != null) {
                        Strings.collections_edit_title
                    } else {
                        Strings.collections_create_title
                    }
                ),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge
            )
        },
        navigationIcon = {
            IconButton(onClick = onCancel) {
                Icon(
                    painter = painterResource(Drawables.arrow_back_24dp),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        actions = {
            val containerColor = if (viewState.isValid) {
                MaterialTheme.colorScheme.primary
            } else {
                CustomTheme.colors.disabledContent
            }

            FilledTonalButton(
                onClick = { onSave() },
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = containerColor),
            ) {
                Text(
                    text = stringResource(Strings.save),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        },
    )
}

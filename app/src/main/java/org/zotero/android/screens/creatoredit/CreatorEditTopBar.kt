package org.zotero.android.screens.creatoredit

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
internal fun CreatorEditTopBar(
    onBack: () -> Unit,
    onSave: () -> Unit,
    viewState: CreatorEditViewState
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        title = {
            val localizedType = viewState.creator?.localizedType?.lowercase() ?: ""
            val titleText = if (viewState.isEditing) {
                stringResource(Strings.edit_creator, localizedType)
            } else {
                stringResource(Strings.add_creator, localizedType)
            }

            Text(
                text = titleText,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
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
                onClick = onSave,
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

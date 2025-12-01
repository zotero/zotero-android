package org.zotero.android.screens.citbibexport

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
internal fun CitBibExportTopBar(
    onCancel: () -> Unit,
    onDone: () -> Unit,
    isDoneButtonEnabled: Boolean,
    isLoading: Boolean,
) {
    val containerColor = if (isDoneButtonEnabled) {
        MaterialTheme.colorScheme.primary
    } else {
        CustomTheme.colors.disabledContent
    }
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        title = {
            Text(
                text = stringResource(Strings.share),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge
            )
        },
        navigationIcon = {
            IconButton(onClick = onCancel) {
                Icon(
                    painter = painterResource(Drawables.ic_close_24dp),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        actions = {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(20.dp))
            } else {
                FilledTonalButton(
                    onClick = { onDone() },
                    enabled = isDoneButtonEnabled,
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = containerColor)
                ) {
                    Text(
                        text = stringResource(Strings.done),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    )
}
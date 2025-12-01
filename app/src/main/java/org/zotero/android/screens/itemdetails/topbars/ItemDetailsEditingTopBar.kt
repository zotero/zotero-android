package org.zotero.android.screens.itemdetails.topbars

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
import org.zotero.android.screens.itemdetails.data.DetailType
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings

@Composable
internal fun ItemDetailsEditingTopBar(
    type: DetailType,
    onViewOrEditClicked: () -> Unit,
    onCancelOrBackClicked: () -> Unit,
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        title = {
            Text(
                text = "",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge
            )
        },
        navigationIcon = {
            IconButton(onClick = {
                if (type is DetailType.creation || type is DetailType.duplication) {
                    onCancelOrBackClicked()
                } else {
                    onViewOrEditClicked()
                }
            }) {
                Icon(
                    painter = painterResource(Drawables.arrow_back_24dp),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        actions = {
            FilledTonalButton(
                onClick = onViewOrEditClicked,
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text(
                    text = stringResource(Strings.save),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
        },
    )
}
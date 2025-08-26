package org.zotero.android.uicomponents.button

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun CustomFilledButton(
    text: String,
    onClick: () -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(
        modifier = modifier,
        onClick = { if (!isLoading) onClick() },
        shapes = ButtonDefaults.shapes(),
        colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.primary),
    ) {
        if (isLoading) {
            ButtonLoadingIndicator(MaterialTheme.colorScheme.onPrimary)
        } else {
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleMedium
            )
        }

    }

}
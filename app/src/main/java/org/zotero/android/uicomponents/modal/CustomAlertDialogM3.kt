package org.zotero.android.uicomponents.modal

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.DialogProperties

@Composable
fun CustomAlertDialogM3(
    title: String,
    description: String,
    confirmButton: CustomAlertDialogM3ActionConfig,
    dismissButton: CustomAlertDialogM3ActionConfig? = null,
    dismissOnClickOutside: Boolean = true,
    onDismiss: () -> Unit,
) {
    val dismissButtonCallback: @Composable (() -> Unit)? = dismissButton?.let {
         {
            TextButton(onClick = {
                onDismiss()
                dismissButton.onClick()
            }, shapes = ButtonDefaults.shapes()) {
                Text(
                    text = dismissButton.text,
                    color = dismissButton.textColor ?: MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnClickOutside = dismissOnClickOutside,
            dismissOnBackPress = dismissOnClickOutside
        ),
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                confirmButton.onClick()
            }, shapes = ButtonDefaults.shapes()) {
                Text(
                    text = confirmButton.text,
                    color = confirmButton.textColor ?: MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        },
        dismissButton = dismissButtonCallback
    )
}

data class CustomAlertDialogM3ActionConfig(
    val text: String,
    val textColor: Color? = null,
    val onClick: () -> Unit = {},
)
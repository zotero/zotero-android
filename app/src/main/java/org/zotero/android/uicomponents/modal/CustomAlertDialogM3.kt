package org.zotero.android.uicomponents.modal

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun CustomAlertDialogM3(
    title: String,
    description: String,
    leftButtonText: String,
    leftButtonColor: Color,
    onLeftButtonClicked: () -> Unit = {},
    rightButtonText: String,
    rightButtonColor: Color,
    onRightButtonClicked: () -> Unit = {},
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
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
                onLeftButtonClicked()
            }, shapes = ButtonDefaults.shapes()) {
                Text(
                    text = leftButtonText,
                    color = leftButtonColor,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onDismiss()
                onRightButtonClicked()
            }, shapes = ButtonDefaults.shapes()) {
                Text(
                    text = rightButtonText,
                    color = rightButtonColor,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    )
}
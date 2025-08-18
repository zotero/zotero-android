package org.zotero.android.uicomponents.snackbar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.androidText
import org.zotero.android.uicomponents.snackbar.SnackbarMessage.ErrorMessageString
import org.zotero.android.uicomponents.snackbar.SnackbarMessage.InfoMessage
import org.zotero.android.uicomponents.theme.CustomPalette
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
fun ErrorSnackbar(
    errorMessageString: ErrorMessageString,
    contentColor: Color
) {
    Snackbar(
        modifier = Modifier.padding(12.dp),
        containerColor = CustomTheme.colors.error,
        shape = RoundedCornerShape(size = 16.dp),
        action = {
            SnackbarAction(
                snackbarMessage = errorMessageString,
                contentColor = contentColor
            )
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = Drawables.ic_alert_icon),
                contentDescription = null,
                tint = CustomPalette.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = errorMessageString.message,
                style = CustomTheme.typography.h4,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun InfoSnackbar(
    infoMessage: InfoMessage,
    contentColor: Color
) {
    Snackbar(
        modifier = Modifier.padding(12.dp),
        containerColor = CustomTheme.colors.primaryContent,
        shape = RoundedCornerShape(size = 16.dp),
        action = {
            SnackbarAction(
                snackbarMessage = infoMessage,
                contentColor = contentColor
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            val textColor = if (CustomTheme.colors.isLight) {
                CustomPalette.White
            } else {
                CustomPalette.Charcoal
            }
            Text(
                text = androidText(infoMessage.title),
                style = CustomTheme.typography.h4,
                color = textColor
            )
            infoMessage.description?.let {
                Text(text = androidText(it), color = textColor)
            }
        }
    }
}

@Composable
private fun SnackbarAction(
    snackbarMessage: SnackbarMessage,
    contentColor: Color,
) {
    snackbarMessage.actionLabel?.let { actionLabel ->
        TextButton(
            colors = ButtonDefaults.textButtonColors(contentColor = contentColor),
            onClick = { snackbarMessage.performAction?.invoke() },
            content = {
                Text(
                    text = androidText(actionLabel),
                    style = CustomTheme.typography.h3,
                    modifier = Modifier.align(Alignment.Bottom)
                )
            },
        )
    }
}

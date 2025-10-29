package org.zotero.android.screens.share.bottompanel

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import org.zotero.android.screens.share.ShareViewModel
import org.zotero.android.translator.data.AttachmentState
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun ShareFailureLabel(
    viewModel: ShareViewModel,
    error: AttachmentState.Error,
    hasItem: Boolean
) {
    var message = viewModel.errorMessage(error) ?: return
    val textColor: Color
    val textAlignment: TextAlign

    if (error.isFatal) {
        textColor = MaterialTheme.colorScheme.error
        textAlignment = TextAlign.Center
    } else {
        when (error) {
            AttachmentState.Error.downloadedFileNotPdf, AttachmentState.Error.apiFailure -> {
                textAlignment = TextAlign.Center
            }

            is AttachmentState.Error.quotaLimit -> {
                textAlignment = TextAlign.Left
            }

            else -> {
                if (!hasItem) {
                    message += "\n" + stringResource(id = Strings.errors_shareext_failed_additional)
                }
                textAlignment = TextAlign.Center
            }
        }
        textColor = CustomTheme.colors.secondaryContent
    }

    Text(
        modifier = Modifier
            .fillMaxWidth(),
        text = message,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = textAlignment,
        color = textColor,
    )
}
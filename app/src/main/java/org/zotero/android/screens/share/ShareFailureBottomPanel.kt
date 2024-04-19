package org.zotero.android.screens.share

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.zotero.android.screens.share.data.ItemPickerState
import org.zotero.android.translator.data.AttachmentState
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.theme.CustomPalette
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun ShareFailureBottomPanel(
    viewModel: ShareViewModel,
    state: AttachmentState,
    itemState: ItemPickerState?,
    hasItem: Boolean,
    isSubmitting: Boolean
) {
    if (itemState != null && itemState.picked == null) {
        return
    }
    Box(modifier = Modifier.fillMaxWidth()) {

        val message: String?
        val showActivityIndicator: Boolean

        if (isSubmitting) {
            message = null
            showActivityIndicator = false
        } else {
            when (state) {
                AttachmentState.decoding -> {
                    message = stringResource(id = Strings.shareext_decoding_attachment)
                    showActivityIndicator = true
                }

                AttachmentState.processed -> {
                    message = null
                    showActivityIndicator = false
                }

                is AttachmentState.translating -> {
                    message = state.message
                    showActivityIndicator = true
                }

                is AttachmentState.downloading -> {
                    message = null
                    showActivityIndicator = true
                }

                is AttachmentState.failed -> {
                    message = null
                    showActivityIndicator = false
                    ShareFailureLabel(viewModel = viewModel, error = state.e, hasItem = hasItem)
                }
            }
        }
        if (message != null) {
            ShareBottomProgressContainer(
                message = message,
                showActivityIndicator = showActivityIndicator
            )
        }
    }


}

@Composable
private fun BoxScope.ShareBottomProgressContainer(message: String, showActivityIndicator: Boolean) {

    Row(modifier = Modifier.align(Alignment.Center)) {
        if (showActivityIndicator) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = CustomTheme.colors.secondaryContent,
                strokeWidth = 2.dp,
            )
            Spacer(modifier = Modifier.width(16.dp))
        }

        Text(
            modifier = Modifier,
            text = message,
            style = CustomTheme.typography.newBody,
            color = CustomTheme.colors.secondaryContent,
        )
    }

}

@Composable
private fun ShareFailureLabel(
    viewModel: ShareViewModel,
    error: AttachmentState.Error,
    hasItem: Boolean
) {
    var message = viewModel.errorMessage(error) ?: return
    val textColor: Color
    val textAlignment: TextAlign

    if (error.isFatal) {
        textColor = CustomPalette.ErrorRed
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
        style = CustomTheme.typography.newBody,
        textAlign = textAlignment,
        color = textColor,
    )
}
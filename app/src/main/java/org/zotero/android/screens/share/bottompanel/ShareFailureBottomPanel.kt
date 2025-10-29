package org.zotero.android.screens.share.bottompanel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.zotero.android.screens.share.ShareViewModel
import org.zotero.android.screens.share.data.ItemPickerState
import org.zotero.android.translator.data.AttachmentState
import org.zotero.android.uicomponents.Strings

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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {

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

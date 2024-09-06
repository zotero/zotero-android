package org.zotero.android.uicomponents.modal
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.zotero.android.R
import org.zotero.android.architecture.content.AndroidText
import org.zotero.android.architecture.content.StringId
import org.zotero.android.uicomponents.androidText
import org.zotero.android.uicomponents.button.ButtonLoadingIndicator
import org.zotero.android.uicomponents.button.PrimaryButton
import org.zotero.android.uicomponents.button.SecondaryButton
import org.zotero.android.uicomponents.button.TextButton
import org.zotero.android.uicomponents.snackbar.SnackbarMessage
import org.zotero.android.uicomponents.theme.CustomPalette
import org.zotero.android.uicomponents.theme.CustomTheme

/**
 * This component is a bottom sheet modal.
 *
 * [modalDialog] tells the component how to draw itself and what should be the
 * content and behavior if the modal. see [ModalDialog] for different types of
 * standard dialogs (confirmation, info, etc.)
 *
 * [customContent] is an optional parameter used to draw non-standard UI on the
 * bottom sheet in addition to the standard content.
 *
 * [shouldDismiss] is an optional parameter to send the dismiss signal from
 * the place of usage.
 *
 * [snackbarMessage] is an optional parameter to show a snackbar message within
 * the area of your modal.
 */
@Composable
fun BottomModalDialog(
    modalDialog: ModalDialog,
    modifier: Modifier = Modifier,
    customContent: @Composable ColumnScope.() -> Unit = {},
    shouldDismiss: Boolean = false,
    snackbarMessage: SnackbarMessage? = null,
) {
    CustomModalBottomSheet(
        modifier = modifier,
        sheetContent = {
            ModalDialogContent(modalDialog, customContent)
        },
        onCollapse = modalDialog.onDismiss,
        shouldCollapse = shouldDismiss,
        snackbarMessage = snackbarMessage,
    )
}

@Composable
fun ModalDialogContent(
    modalDialog: ModalDialog,
    customContent: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier = Modifier.padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DialogIcon(modalDialog)
        Title(modalDialog.title)
        Message(modalDialog.message)
        customContent()
        Buttons(modalDialog)
    }
}

@Composable
private fun DialogIcon(modalDialog: ModalDialog) {
    val iconResId = modalDialog.iconResId ?: return

    val iconTint = when (modalDialog) {
        is ModalDialog.Info, is ModalDialog.Custom -> CustomTheme.colors.dynamicTheme.primaryColor
        is ModalDialog.Confirmation -> CustomTheme.colors.error
    }
    val iconBackground = when (modalDialog) {
        is ModalDialog.Info, is ModalDialog.Custom -> CustomTheme.colors.dynamicTheme.shadeFour
        is ModalDialog.Confirmation -> CustomTheme.colors.errorSecondary
    }
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(
                color = iconBackground,
                shape = RoundedCornerShape(16.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.size(32.dp),
            painter = painterResource(iconResId),
            contentDescription = null,
            tint = iconTint,
        )
    }
}

@Composable
private fun Title(stringId: AndroidText?) {
    if (stringId != null) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp),
            text = androidText(stringId),
            style = CustomTheme.typography.h1,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Message(stringId: AndroidText?) {
    if (stringId != null) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp),
            text = androidText(stringId),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Buttons(modalDialog: ModalDialog) = when (modalDialog) {
    is ModalDialog.Info -> {
        PrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp, start = 16.dp, end = 16.dp),
            text = androidText(modalDialog.action.textResId),
            onClick = modalDialog.action.onClick,
            isLoading = modalDialog.action.isLoading,
        )

        modalDialog.secondaryAction?.let { action ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp)
            ) {
                val textColor = CustomTheme.colors.dynamicTheme.primaryColor
                if (action.isLoading) {
                    ButtonLoadingIndicator(
                        color = textColor,
                    )
                } else {
                    TextButton(
                        text = androidText(action.textResId),
                        onClick = action.onClick,
                    )
                }
            }
        }
    }
    is ModalDialog.Confirmation -> {
        PrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp, start = 16.dp, end = 16.dp),
            text = androidText(modalDialog.confirmAction.textResId),
            onClick = modalDialog.confirmAction.onClick,
            backgroundColor = CustomTheme.colors.error,
            contentColor = CustomPalette.White,
            isLoading = modalDialog.confirmAction.isLoading,
        )
        SecondaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp),
            text = androidText(modalDialog.cancelAction.textResId),
            onClick = modalDialog.cancelAction.onClick,
            isLoading = modalDialog.cancelAction.isLoading,
        )
    }
    is ModalDialog.Custom -> {
        PrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp),
            text = androidText(modalDialog.action.textResId),
            onClick = modalDialog.action.onClick,
            isLoading = modalDialog.action.isLoading,
        )
    }
}

sealed class ModalDialog {
    abstract val title: AndroidText?
    abstract val message: AndroidText?
    abstract val onDismiss: () -> Unit
    abstract val iconResId: Int?

    data class Info(
        override val title: StringId? = null,
        override val message: StringId? = null,
        override val onDismiss: () -> Unit,
        override val iconResId: Int? = R.drawable.info_24px,
        val action: Action,
        val secondaryAction: Action? = null,
    ) : ModalDialog()

    data class Confirmation(
        override val title: StringId? = null,
        override val message: StringId? = null,
        override val onDismiss: () -> Unit,
        override val iconResId: Int = R.drawable.ic_exclamation_24dp,
        val confirmAction: Action,
        val cancelAction: Action,
    ) : ModalDialog()

    /**
     * Use this subtype when custom UI needs to be displayed in a modal dialog.
     * */
    data class Custom(
        override val title: AndroidText? = null,
        override val message: AndroidText? = null,
        override val onDismiss: () -> Unit,
        override val iconResId: Int,
        val action: Action,
    ) : ModalDialog()

    data class Action(
        val textResId: StringId,
        val onClick: () -> Unit = {},
        val isLoading: Boolean = false,
    )
}
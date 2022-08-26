package org.zotero.android.uicomponents.snackbar

import androidx.annotation.StringRes
import org.zotero.android.architecture.content.StringId

sealed class SnackbarMessage {

    abstract val actionLabel: StringId?
    abstract val performAction: (() -> Unit)?
    abstract val onDismiss: () -> Unit

    class ErrorMessageString(
        val message: String,
        override val actionLabel: StringId? = null,
        override val performAction: (() -> Unit)? = null,
        override val onDismiss: () -> Unit,
    ) : SnackbarMessage()

    class InfoMessage(
        val title: StringId,
        val description: StringId? = null,
        override val actionLabel: StringId? = null,
        override val performAction: (() -> Unit)? = null,
        override val onDismiss: () -> Unit,
    ) : SnackbarMessage() {
        constructor(
            @StringRes titleRes: Int,
            @StringRes descriptionRes: Int? = null,
            actionLabelRes: Int? = null,
            performAction: (() -> Unit)? = null,
            onDismiss: () -> Unit,
        ) : this(
            title = titleRes.asStringId,
            description = descriptionRes?.asStringId,
            actionLabel = actionLabelRes?.asStringId,
            performAction = performAction,
            onDismiss = onDismiss
        )
    }
}

private val @receiver:StringRes Int.asStringId: StringId
    get() = StringId(this)

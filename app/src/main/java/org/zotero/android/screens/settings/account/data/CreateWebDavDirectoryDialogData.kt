package org.zotero.android.screens.settings.account.data

import org.zotero.android.api.network.CustomResult

data class CreateWebDavDirectoryDialogData(
    val url: String,
    val error: CustomResult.GeneralError,
)
package org.zotero.android.screens.addnote.data

import com.google.gson.annotations.SerializedName

data class WebViewUpdateResponse(
    @SerializedName("message") val message: WebViewUpdateMessage
)
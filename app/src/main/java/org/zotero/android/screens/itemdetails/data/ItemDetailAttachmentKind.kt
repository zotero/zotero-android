package org.zotero.android.screens.itemdetails.data

sealed class ItemDetailAttachmentKind {
    object default : ItemDetailAttachmentKind()
    data class inProgress(val progressInHundreds: Int) : ItemDetailAttachmentKind()
    data class failed(val error: Throwable) : ItemDetailAttachmentKind()
    object disabled : ItemDetailAttachmentKind()
}
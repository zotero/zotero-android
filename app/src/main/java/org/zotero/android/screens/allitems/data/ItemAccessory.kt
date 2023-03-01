package org.zotero.android.screens.allitems.data

import org.zotero.android.database.objects.Attachment

sealed class ItemAccessory {
    data class attachment(val attachment: Attachment): ItemAccessory()
    data class doi(val doi: String): ItemAccessory()
    data class url(val url: String): ItemAccessory()

    val attachmentGet: Attachment?
        get() {
            when (this) {
                is attachment -> return this.attachment
                is doi, is url -> return null
            }
        }
}
package org.zotero.android.dashboard.data

import org.zotero.android.architecture.database.objects.Attachment

sealed class ItemAccessory {
    data class attachment(val attachment: Attachment): ItemAccessory()
    data class doi(val str: String): ItemAccessory()
    data class url(val url: String): ItemAccessory()
}
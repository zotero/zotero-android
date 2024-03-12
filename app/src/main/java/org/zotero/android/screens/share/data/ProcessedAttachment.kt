package org.zotero.android.screens.share.data

import org.zotero.android.api.pojo.sync.ItemResponse
import java.io.File

sealed interface ProcessedAttachment {
    data class item(val item: ItemResponse) : ProcessedAttachment
    data class itemWithAttachment(
        val item: ItemResponse,
        val attachment: Map<String, Any>,
        val attachmentFile: File
    ) : ProcessedAttachment

    data class file(
        val file: File,
        val fileName: String
    ) : ProcessedAttachment
}
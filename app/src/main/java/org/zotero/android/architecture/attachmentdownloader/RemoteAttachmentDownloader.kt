package org.zotero.android.architecture.attachmentdownloader

import org.zotero.android.architecture.database.objects.Attachment
import org.zotero.android.sync.LibraryIdentifier

class RemoteAttachmentDownloader {
    data class Download(
        val key: String,
        val parentKey: String,
        val libraryId: LibraryIdentifier
    )

    data class Update(
        val download: Download,
        val kind: Kind
    ) {
        sealed class Kind {
            data class progress(val progressInHundreds: Int): Kind()
            data class ready(val attachment: Attachment): Kind()
            object failed: Kind()
            object cancelled: Kind()
        }
    }


}
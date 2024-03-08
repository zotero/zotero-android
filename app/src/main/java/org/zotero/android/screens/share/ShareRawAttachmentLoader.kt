package org.zotero.android.screens.share

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import org.zotero.android.translator.data.AttachmentState
import org.zotero.android.translator.data.RawAttachment
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShareRawAttachmentLoader @Inject constructor() {

    fun loadAttachment(stateHandle: SavedStateHandle): RawAttachment {
        val urlPath = stateHandle.get<String>(Intent.EXTRA_TEXT)
        if (urlPath != null) {
            return RawAttachment.remoteUrl(urlPath)
        }
        val fileContentUri = stateHandle.get<Uri>(Intent.EXTRA_STREAM)
        if (fileContentUri != null) {
            return RawAttachment.fileUrl(fileContentUri)
        }
        throw AttachmentState.Error.cantLoadWebData
    }

}
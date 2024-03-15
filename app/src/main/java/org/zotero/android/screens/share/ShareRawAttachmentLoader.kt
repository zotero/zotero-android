package org.zotero.android.screens.share

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.pspdfkit.utils.getSupportParcelable
import org.zotero.android.architecture.Result
import org.zotero.android.translator.data.AttachmentState
import org.zotero.android.translator.data.RawAttachment
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShareRawAttachmentLoader @Inject constructor() {

    private lateinit var loadedAttachment: Result<RawAttachment>

    fun loadAttachment(bundleExtras: Bundle) {
        val urlPath = bundleExtras.getString(Intent.EXTRA_TEXT)
        if (urlPath != null) {
            loadedAttachment = Result.Success(RawAttachment.remoteUrl(urlPath))
            return
        }
        val fileContentUri = bundleExtras.getSupportParcelable(Intent.EXTRA_STREAM, Uri::class.java)
        if (fileContentUri != null) {
            loadedAttachment = Result.Success(RawAttachment.fileUrl(fileContentUri))
            return
        }
        loadedAttachment = Result.Failure(AttachmentState.Error.cantLoadWebData)
    }

    fun getLoadedAttachmentResult(): RawAttachment {
        return when (val local = loadedAttachment) {
            is Result.Failure -> throw local.exception
            is Result.Success -> local.value
        }
    }

}
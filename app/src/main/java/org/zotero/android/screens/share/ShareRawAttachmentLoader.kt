package org.zotero.android.screens.share

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.net.toUri
import com.pspdfkit.utils.getSupportParcelable
import org.zotero.android.architecture.Result
import org.zotero.android.translator.data.AttachmentState
import org.zotero.android.translator.data.RawAttachment
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShareRawAttachmentLoader @Inject constructor() {

    private lateinit var loadedAttachment: Result<RawAttachment>
    
    fun loadFromIntent(intent: Intent) {
        val bundleExtras = intent.extras
        if (bundleExtras != null
            && (bundleExtras.containsKey(Intent.EXTRA_STREAM)
                    || bundleExtras.containsKey(Intent.EXTRA_TEXT))
        ) {
            loadFromIntentExtras(bundleExtras)
        } else {
            loadFromIntentData(intent.data)
        }
    }

    private fun loadFromIntentData(data: Uri?) {
        if (data == null) {
            loadedAttachment = Result.Failure(AttachmentState.Error.cantLoadWebData)
        } else {
            loadedAttachment = Result.Success(RawAttachment.fileUrl(data))
        }

    }

    private fun loadFromIntentExtras(bundleExtras: Bundle) {
        val urlPath = bundleExtras.getString(Intent.EXTRA_TEXT)
        if (urlPath != null) {
            val lastPathSegment = urlPath.toUri().lastPathSegment
            if (lastPathSegment?.contains(".") == true) {
                loadedAttachment =
                    Result.Success(RawAttachment.remoteFileUrl(
                        url = urlPath,
                        contentType = "",
                        cookies = "",
                        userAgent = "",
                        referrer = ""
                    ))
            } else {
                loadedAttachment = Result.Success(RawAttachment.remoteUrl(urlPath))
            }
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

    fun doesIntentContainShareData(intent: Intent): Boolean {
        val bundleExtras = intent.extras
        val urlPath = bundleExtras?.getString(Intent.EXTRA_TEXT)
        val fileContentUri = bundleExtras?.getSupportParcelable(Intent.EXTRA_STREAM, Uri::class.java)
        val dataPath = intent.data
        return urlPath != null || fileContentUri != null || dataPath != null
    }

}
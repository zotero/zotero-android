package org.zotero.android.screens.share

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.pspdfkit.utils.getSupportParcelable
import kotlinx.coroutines.withContext
import org.zotero.android.architecture.Result
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.helpers.GetUriDetailsUseCase
import org.zotero.android.translator.data.AttachmentState
import org.zotero.android.translator.data.RawAttachment
import javax.inject.Inject
import javax.inject.Singleton

//MUST be singleton as it shares the state between multiple ViewModels
@Singleton
class ShareRawAttachmentLoader @Inject constructor(
    private val getUriDetailsUseCase: GetUriDetailsUseCase,
    private val application: Application,
    private val dispatchers: Dispatchers
) {

    private lateinit var loadedAttachment: Result<RawAttachment>
    
    suspend fun maybeLoadFromIntent(intent: Intent) = withContext(dispatchers.default) {
        if (!doesIntentContainShareData(intent)) {
            return@withContext false
        }

        val bundleExtras = intent.extras
        this@ShareRawAttachmentLoader.loadedAttachment = if (bundleExtras != null
            && (bundleExtras.containsKey(Intent.EXTRA_STREAM)
                    || bundleExtras.containsKey(Intent.EXTRA_TEXT))
        ) {
            loadFromIntentExtras(bundleExtras)
        } else {
            loadFromIntentData(intent.data)
        }
        return@withContext true
    }

    private fun loadFromIntentData(data: Uri?): Result<RawAttachment.fileUrl> {
        return if (data == null) {
            Result.Failure(AttachmentState.Error.cantLoadWebData)
        } else {
            Result.Success(generateRawAttachmentFileUrl(data))
        }
    }

    private fun loadFromIntentExtras(bundleExtras: Bundle): Result<RawAttachment> {
        val urlPath = bundleExtras.getString(Intent.EXTRA_TEXT)
        if (urlPath != null) {
            val listOfUrls = extractUrls(urlPath)
            if (listOfUrls.isNotEmpty()) {
                return Result.Success(RawAttachment.remoteUrl(listOfUrls[0]))
            }
        }
        val fileContentUri = bundleExtras.getSupportParcelable(Intent.EXTRA_STREAM, Uri::class.java)
        if (fileContentUri != null) {
            return Result.Success(generateRawAttachmentFileUrl(fileContentUri))
        }
        return Result.Failure(AttachmentState.Error.cantLoadWebData)
    }

    private fun generateRawAttachmentFileUrl(uri: Uri): RawAttachment.fileUrl {
        val fileName = getUriDetailsUseCase.getFullName(uri)
        val fileExtension = getUriDetailsUseCase.getExtension(uri)
        val uriInputStream = application.contentResolver.openInputStream(uri)!!
        return RawAttachment.fileUrl(fileName, fileExtension, uriInputStream)
    }

    private fun extractUrls(text: String): List<String> {
        val urlPattern = Regex(
            """(https?://)?(www\.)?[\w-]+(\.[\w-]+)+\.?(:\d+)?(/[\w\-._~:/?#\[\]@!$&'()*+,;=]*)?"""
        )
        return urlPattern.findAll(text).map { it.value }.toList()
    }

    fun getLoadedAttachmentResult(): RawAttachment {
        return when (val local = loadedAttachment) {
            is Result.Failure -> throw local.exception
            is Result.Success -> local.value
        }
    }

    private fun doesIntentContainShareData(intent: Intent): Boolean {
        val bundleExtras = intent.extras
        val urlPath = bundleExtras?.getString(Intent.EXTRA_TEXT)
        val fileContentUri = bundleExtras?.getSupportParcelable(Intent.EXTRA_STREAM, Uri::class.java)
        val dataPath = intent.data
        return urlPath != null || fileContentUri != null || dataPath != null
    }

}
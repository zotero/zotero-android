package org.zotero.android.helpers

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.internal.closeQuietly
import org.apache.commons.io.FileUtils
import org.zotero.android.translator.data.AttachmentState
import timber.log.Timber
import java.io.File
import javax.inject.Inject

class GetUriDetailsUseCase @Inject constructor(
    private val application: Application,
    private val dispatcher: CoroutineDispatcher
) {
    fun getFullName(uri: Uri): String? {
        application.contentResolver.query(
            uri,
            null,
            null,
            null,
            null
        ).use { cursor ->
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
            }
        }
        return null
    }

    fun getExtension(uri: Uri): String? {
        val extension: String?
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            val mime = MimeTypeMap.getSingleton()
            extension = mime.getExtensionFromMimeType(application.contentResolver.getType(uri))
        } else {
            extension = MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(File(uri.path)).toString())
        }
        return extension
    }

    suspend fun getMimeType(uri: String): MimeType? = withContext(dispatcher) {
        val contentUri = Uri.parse(uri)
        application.contentResolver.getType(contentUri)
    }

    suspend fun copyFile(fromUri: Uri, toFile: File) = withContext(dispatcher) {
        Timber.i("GetUriDetailsUseCase: copy file to attachment folder")

        try {
            val uriInputStream = application.contentResolver.openInputStream(fromUri)!!
            FileUtils.copyInputStreamToFile(uriInputStream, toFile)
            uriInputStream.closeQuietly()
        } catch (error: Exception) {
            Timber.e(error, "GetUriDetailsUseCase: can't copy file")
            throw AttachmentState.Error.fileMissing
        }
    }
}

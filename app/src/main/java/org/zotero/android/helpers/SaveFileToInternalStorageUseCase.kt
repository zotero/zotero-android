package org.zotero.android.helpers

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.zotero.android.files.FileStore
import java.io.File
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject

typealias ContentUri = Any

@Suppress("TooGenericExceptionCaught")
class SaveFileToInternalStorageUseCase @Inject constructor(
    private val fileRepo: FileStore,
    private val app: Application,
    private val dispatcher: CoroutineDispatcher
) {
    suspend fun execute(stringUri: String): org.zotero.android.architecture.Result<InternalFile> = withContext(dispatcher) {
        try {
            val uri = Uri.parse(stringUri)
            org.zotero.android.architecture.Result.Success(createInternalFile(uri))
        } catch (e: Exception) {
            org.zotero.android.architecture.Result.Failure(e)
        }
    }

    suspend fun execute(contentUri: ContentUri): org.zotero.android.architecture.Result<InternalFile> =
        withContext(dispatcher) {
            if (contentUri !is Uri) {
                return@withContext org.zotero.android.architecture.Result.Failure(
                    IllegalArgumentException("Expected Uri, got $contentUri")
                )
            }

            try {
                org.zotero.android.architecture.Result.Success(createInternalFile(contentUri))
            } catch (e: Exception) {
                org.zotero.android.architecture.Result.Failure(e)
            }
        }

    private fun createInternalFile(uri: Uri): InternalFile {
        val inputStream: InputStream = checkNotNull(fileRepo.openInputStream(uri)) {
            "Failed to open InputStream for uri $uri"
        }

        val mimeType: MimeType = checkNotNull(fileRepo.getType(uri)) {
            "Failed to get MimeType for uri $uri"
        }

        val cacheDir: File = fileRepo.cache()
        val internalFile = File(cacheDir, getFileName(uri))
        if (internalFile.exists()) {
            internalFile.delete()
        }

        internalFile.createNewFile()

        inputStream.use { inStream ->
            val outputStream = internalFile.outputStream()
            outputStream.use { outStream -> inStream.copyTo(outStream) }
        }

        return InternalFile(
            mimeType = mimeType,
            file = internalFile,
            canonicalPath = internalFile.canonicalPath
        )
    }

    private fun getFileName(uri: Uri): String {
        app.contentResolver.query(
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
        return UUID.randomUUID().toString()
    }

    companion object {
        private const val BYTES_IN_A_MB = 1024 * 1024
    }
}

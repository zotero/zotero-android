package org.zotero.android.documentworker

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.documentworker.web.DocumentWorkerWebCallChainExecutor
import org.zotero.android.files.FileStore
import org.zotero.android.helpers.FileHelper
import java.io.File
import javax.inject.Inject

@ViewModelScoped
class DocumentWorkerController @Inject constructor(
    private val context: Context,
    private val dispatchers: Dispatchers,
    private val gson: Gson,
    private val fileStore: FileStore,
) {
    data class SDTPack(
        val bytes: ByteArray,
        val packVersion: Int,
        val schemaMajorVersion: Int,
    )

    private data class DocumentWorkerMetadata(
        @SerializedName("SDT_SCHEMA_VERSION") val schemaVersion: String,
        @SerializedName("SDT_PACK_VERSION") val packVersion: Int,
    )

    private var executor: DocumentWorkerWebCallChainExecutor? = null

    private fun ensureExecutor(): DocumentWorkerWebCallChainExecutor {
        var current = executor
        if (current == null) {
            current = DocumentWorkerWebCallChainExecutor(
                context = context,
                dispatchers = dispatchers,
                gson = gson,
                fileStore = fileStore,
            )
            current.start()
            executor = current
        }
        return current
    }

    private fun readMetadata(): DocumentWorkerMetadata {
        val file = File(fileStore.documentWorkerDirectory(), "metadata.json")
        return gson.fromJson(file.readText(), DocumentWorkerMetadata::class.java)
    }

    private fun cacheFile(sourceHash: String): File {
        return File(fileStore.sdtCacheDirectory(), "$sourceHash.sdt")
    }

    suspend fun getOrGenerateSDTPack(
        file: File,
        contentType: String = "application/pdf",
        password: String? = null,
        onProgress: (Int) -> Unit = {},
    ): SDTPack = withContext(dispatchers.io) {
        val metadata = readMetadata()
        val schemaMajorVersion = metadata.schemaVersion.substringBefore('.').toIntOrNull() ?: 0
        val sourceHash = FileHelper.cachedMD5(file)
            ?: throw IllegalStateException("Unable to hash file for SDT generation: ${file.absolutePath}")
        val cache = cacheFile(sourceHash)
        val bytes = if (cache.exists()) {
            cache.readBytes()
        } else {
            val generated = ensureExecutor().generateSDT(
                file = file,
                contentType = contentType,
                sourceHash = sourceHash,
                password = password,
                onProgress = onProgress,
            )
            cache.writeBytes(generated)
            generated
        }
        SDTPack(
            bytes = bytes,
            packVersion = metadata.packVersion,
            schemaMajorVersion = schemaMajorVersion,
        )
    }
}
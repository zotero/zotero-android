package org.zotero.android.loaders.documentworker

import android.content.Context
import org.zotero.android.architecture.Defaults
import org.zotero.android.files.FileStore
import org.zotero.android.helpers.FileHelper
import org.zotero.android.helpers.Unzipper
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

//Must be singleton, used by Controller
@Singleton
class DocumentWorkerLoader @Inject constructor(
    private val context: Context,
    private val defaults: Defaults,
    private val unzipper: Unzipper,
    private val fileStore: FileStore,
) {
    fun updateDocumentWorkerIfNeeded() {
        Timber.i("DocumentWorkerLoader: update document worker")
        try {
            updateHostPage()
            updateDocumentWorkerFromBundle()
        } catch (error: Exception) {
            Timber.e(error, "DocumentWorkerLoader: can't update from bundle")
        }
    }

    private fun updateDocumentWorkerFromBundle() {
        val hash = loadLastDocumentWorkerHash()
        val isDirectoryEmpty = fileStore.documentWorkerDirectory().listFiles().isNullOrEmpty()
        Timber.i("DocumentWorkerLoader: should update document worker from bundle, isDirectoryEmpty=$isDirectoryEmpty; oldHash=${defaults.getLastDocumentWorkerHash()}; newHash=$hash")
        if (!isDirectoryEmpty && defaults.getLastDocumentWorkerHash() == hash) {
            return
        }
        Timber.i("DocumentWorkerLoader: update document worker from bundle")
        unzipper.unzipStream(
            zipInputStream = context.assets.open("document_worker/document_worker.zip"),
            location = fileStore.documentWorkerDirectory().absolutePath
        )
        defaults.setLastDocumentWorkerHash(hash)
    }

    private fun updateHostPage() {
        val hostPage = File(fileStore.documentWorkerHostDirectory(), "index.html")
        context.assets.open("document_worker_host/index.html").use { inputStream ->
            FileHelper.copyInputStreamToFile(inputStream = inputStream, file = hostPage)
        }
    }

    private fun loadLastDocumentWorkerHash(): String {
        val rawValue = FileHelper.toString(context.assets.open("document_worker/document_worker_hash.txt"))
        return rawValue.trim()
    }
}
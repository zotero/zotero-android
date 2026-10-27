package org.zotero.android.loaders.documentworker

import android.content.Context
import org.zotero.android.architecture.Defaults
import org.zotero.android.files.FileStore
import org.zotero.android.helpers.FileHelper
import org.zotero.android.helpers.Unzipper
import timber.log.Timber
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
    enum class UpdateType(val i: Int) {
        manual(1),
        initial(2),
        startup(3),
        notification(4),
        shareExtension(5);
    }

    sealed class Error : Exception() {
        data class bundleLoading(val exception: Exception) : Error()
        object bundleMissing : Error()

        val isBundleLoadingError: Boolean
            get() {
                return when (this) {
                    is bundleLoading -> {
                        true
                    }

                    else -> {
                        false
                    }
                }
            }
    }

    fun updateDocumentWorkerIfNeeded() {
        _update()
    }

    private fun _update() {
        val type: UpdateType =
            if (defaults.getLastTimestamp() == 0L) {
                UpdateType.initial
            } else {
                UpdateType.startup
            }

        Timber.Forest.i("DocumentWorkerLoader: update document-worker JS")
        try {
            checkFolderIntegrity(type = type)
            updateFromBundle()
            defaults.setLastTimestamp(System.currentTimeMillis() / 1000)
        } catch (error: Exception) {
            process(error = error)
        }
    }

    private fun _updateDocumentWorkerFromBundle(forceUpdate: Boolean) {
        val hash = loadLastDocumentWorkerCommitHash()
        Timber.Forest.i("DocumentWorkerLoader: should update document-worker from bundle, forceUpdate=$forceUpdate; oldHash=${defaults.getLastDocumentWorkerCommitHash()}; newHash=$hash")
        if (!forceUpdate && defaults.getLastDocumentWorkerCommitHash() == hash) {
            return
        }
        Timber.Forest.i("DocumentWorkerLoader: update document-worker from bundle")
        updateDocumentWorker()

        defaults.setLastDocumentWorkerCommitHash(hash)
    }

    private fun updateDocumentWorker() {
        unzipper.unzipStream(
            zipInputStream = context.assets.open("document-worker/document-worker.zip"),
            location = fileStore.documentWorkerDirectory().absolutePath
        )
    }

    private fun loadLastDocumentWorkerCommitHash(): String {
        return loadFromBundle(resource = "document-worker/document-worker_commit_hash.txt", map = { it })
    }


    private inline fun <reified Result> loadFromBundle(
        resource: String,
        map: (String) -> Result
    ): Result {
        try {
            val inputStream = context.assets.open(resource)
            val rawValue = FileHelper.toString(inputStream)
            return map(rawValue.trim().trim { it == '\n' })
        } catch (e: Exception) {
            Timber.Forest.e(e)
            throw Error.bundleMissing
        }
    }

    private fun updateFromBundle() {
        try {
            _updateDocumentWorkerFromBundle(forceUpdate = false)
            val timestamp = loadLastTimestamp()
            if (timestamp > defaults.getLastTimestamp()) {
                defaults.setLastTimestamp(timestamp)
                return
            } else {
                return
            }

        } catch (error: Exception) {
            Timber.Forest.e(error, "DocumentWorkerLoader: can't update from bundle")
            throw Error.bundleLoading(error)
        }
    }

    private fun loadLastTimestamp(): Long {
        return loadFromBundle(resource = "timestamp.txt", map = {
            try {
                return it.toLong()
            } catch (e: Exception) {
                Timber.Forest.e(e)
                throw Error.bundleMissing
            }
        })
    }

    private fun checkFolderIntegrity(type: UpdateType) {
        try {
            if (!fileStore.documentWorkerDirectory().exists()) {
                if (type != UpdateType.initial) {
                    Timber.Forest.e("DocumentWorkerLoader: document-worker directory was missing!")
                }
                fileStore.documentWorkerDirectory().mkdirs()
            }

            if (type == UpdateType.initial) {
                return
            }

            val fileCount = fileStore.documentWorkerDirectory().listFiles()?.size ?: 0

            if (fileCount != 0) {
                return
            }

            defaults.setLastTimestamp(0L)
            defaults.setLastDocumentWorkerCommitHash("")
        } catch (error: Exception) {
            Timber.Forest.e(error, "DocumentWorkerLoader: unable to restore folder integrity")
            throw error
        }

    }

    private fun process(error: Exception) {
        Timber.Forest.e(error, "DocumentWorkerLoader: error")

        val isBundleLoadingError = (error as? Error)?.isBundleLoadingError == true
        if (!isBundleLoadingError) {
            return
        }
    }
}

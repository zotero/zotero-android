package org.zotero.android.pdfworker.loader

import android.content.Context
import org.zotero.android.architecture.Defaults
import org.zotero.android.files.FileStore
import org.zotero.android.helpers.FileHelper
import org.zotero.android.helpers.Unzipper
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfWorkerLoader @Inject constructor(
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

    fun updatePdfWorkerIfNeeded() {
        _update()
    }

    private fun _update() {
        val type: UpdateType =
            if (defaults.getLastTimestamp() == 0L) {
                UpdateType.initial
            } else {
                UpdateType.startup
            }

        Timber.i("PdfWorkerLoader: update pdf-worker JS")
        try {
            checkFolderIntegrity(type = type)
            updateFromBundle()
            defaults.setLastTimestamp(System.currentTimeMillis() / 1000)
        } catch (error: Exception) {
            process(error = error)
        }
    }

    private fun _updatePdfWorkerFromBundle(forceUpdate: Boolean) {
        val hash = loadLastPdfWorkerCommitHash()
        Timber.i("PdfWorkerLoader: should update pdf-worker from bundle, forceUpdate=$forceUpdate; oldHash=${defaults.getLastPdfWorkerCommitHash()}; newHash=$hash")
        if (!forceUpdate && defaults.getLastPdfWorkerCommitHash() == hash) {
            return
        }
        Timber.i("PdfWorkerLoader: update pdf-worker from bundle")
        updatePdfWorker()

        defaults.setLastPdfWorkerCommitHash(hash)
    }

    private fun updatePdfWorker() {
        unzipper.unzipStream(
            zipInputStream = context.assets.open("pdf-worker.zip"),
            location = fileStore.pdfWorkerDirectory().absolutePath
        )
    }

    private fun loadLastPdfWorkerCommitHash(): String {
        return loadFromBundle(resource = "pdf-worker_commit_hash.txt", map = { it })
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
            Timber.e(e)
            throw Error.bundleMissing
        }
    }

    private fun updateFromBundle() {
        try {
            _updatePdfWorkerFromBundle(forceUpdate = false)
            val timestamp = loadLastTimestamp()
            if (timestamp > defaults.getLastTimestamp()) {
                defaults.setLastTimestamp(timestamp)
                return
            } else {
                return
            }

        } catch (error: Exception) {
            Timber.e(error, "PdfWorkerLoader: can't update from bundle")
            throw Error.bundleLoading(error)
        }
    }

    private fun loadLastTimestamp(): Long {
        return loadFromBundle(resource = "timestamp.txt", map = {
            try {
                return it.toLong()
            } catch (e: Exception) {
                Timber.e(e)
                throw Error.bundleMissing
            }
        })
    }

    private fun checkFolderIntegrity(type: UpdateType) {
        try {
            if (!fileStore.pdfWorkerDirectory().exists()) {
                if (type != UpdateType.initial) {
                    Timber.e("PdfWorkerLoader: pdf-worker directory was missing!")
                }
                fileStore.pdfWorkerDirectory().mkdirs()
            }

            if (type == UpdateType.initial) {
                return
            }

            val fileCount = fileStore.pdfWorkerDirectory().listFiles()?.size ?: 0

            if (fileCount != 0) {
                return
            }

            defaults.setLastTimestamp(0L)
            defaults.setLastPdfWorkerCommitHash("")
        } catch (error: Exception) {
            Timber.e(error, "PdfWorkerLoader: unable to restore folder integrity")
            throw error
        }

    }

    private fun process(error: Exception) {
        Timber.e(error, "PdfWorkerLoader: error")

        val isBundleLoadingError = (error as? Error)?.isBundleLoadingError == true
        if (!isBundleLoadingError) {
            return
        }
    }
}
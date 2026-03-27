package org.zotero.android.htmlepub

import android.content.Context
import org.zotero.android.architecture.Defaults
import org.zotero.android.files.FileStore
import org.zotero.android.helpers.FileHelper
import org.zotero.android.helpers.Unzipper
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HtmlEpubReaderLoader @Inject constructor(
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

    fun updateHtmlEpubReaderIfNeeded() {
        _update()
    }

    private fun _update() {
        val type: UpdateType =
            if (defaults.getLastTimestamp() == 0L) {
                UpdateType.initial
            } else {
                UpdateType.startup
            }

        Timber.i("HtmlEpubReaderLoader: update HTML/EPUB reader")
        try {
            checkFolderIntegrity(type = type)
            updateFromBundle()
            defaults.setLastTimestamp(System.currentTimeMillis() / 1000)
        } catch (error: Exception) {
            process(error = error)
        }
    }

    private fun _updateFromBundle(forceUpdate: Boolean) {
        val hash = loadLastReaderCommitHash()
        Timber.i("HtmlEpubReaderLoader: should update HTML/EPUB reader from bundle, forceUpdate=$forceUpdate; oldHash=${defaults.getLastHtmlEpubReaderCommitHash()}; newHash=$hash")
        if (!forceUpdate && defaults.getLastHtmlEpubReaderCommitHash() == hash) {
            return
        }
        Timber.i("HtmlEpubReaderLoader: update HTML/EPUB reader from bundle")
        updateReader()

        defaults.setLastHtmlEpubReaderCommitHash(hash)
    }

    private fun updateReader() {
        unzipper.unzipStream(
            zipInputStream = context.assets.open("reader/reader.zip"),
            location = fileStore.htmlEpubReaderDirectory().absolutePath
        )
    }

    private fun loadLastReaderCommitHash(): String {
        return loadFromBundle(resource = "reader/reader_hash.txt", map = { it })
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
            _updateFromBundle(forceUpdate = false)
            val timestamp = loadLastTimestamp()
            if (timestamp > defaults.getLastTimestamp()) {
                defaults.setLastTimestamp(timestamp)
                return
            } else {
                return
            }

        } catch (error: Exception) {
            Timber.e(error, "HtmlEpubReaderLoader: can't update from bundle")
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
            if (!fileStore.htmlEpubReaderDirectory().exists()) {
                if (type != UpdateType.initial) {
                    Timber.e("HtmlEpubReaderLoader: HTML/EPUB reader directory was missing!")
                }
                fileStore.htmlEpubReaderDirectory().mkdirs()
            }

            if (type == UpdateType.initial) {
                return
            }

            val fileCount = fileStore.htmlEpubReaderDirectory().listFiles()?.size ?: 0

            if (fileCount != 0) {
                return
            }

            defaults.setLastTimestamp(0L)
            defaults.setLastHtmlEpubReaderCommitHash("")
        } catch (error: Exception) {
            Timber.e(error, "HtmlEpubReaderLoader: unable to restore folder integrity")
            throw error
        }

    }

    private fun process(error: Exception) {
        Timber.e(error, "HtmlEpubReaderLoader: error")

        val isBundleLoadingError = (error as? Error)?.isBundleLoadingError == true
        if (!isBundleLoadingError) {
            return
        }
    }
}
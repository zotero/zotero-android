package org.zotero.android.utilities

import android.content.Context
import org.zotero.android.architecture.Defaults
import org.zotero.android.files.FileStore
import org.zotero.android.helpers.FileHelper
import org.zotero.android.helpers.Unzipper
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UtilitiesLoader @Inject constructor(
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

    fun updateUtilitiesIfNeeded() {
        _update()
    }

    private fun _update() {
        val type: UpdateType =
            if (defaults.getLastTimestamp() == 0L) {
                UpdateType.initial
            } else {
                UpdateType.startup
            }

        Timber.i("UtilitiesLoader: update utilities JS")
        try {
            checkFolderIntegrity(type = type)
            updateFromBundle()
            defaults.setLastTimestamp(System.currentTimeMillis() / 1000)
        } catch (error: Exception) {
            process(error = error)
        }
    }

    private fun _updateFromBundle(forceUpdate: Boolean) {
        val hash = loadLastUtilitiesCommitHash()
        Timber.i("UtilitiesLoader: should update utilities from bundle, forceUpdate=$forceUpdate; oldHash=${defaults.getLastUtilitiesCommitHash()}; newHash=$hash")
        if (!forceUpdate && defaults.getLastUtilitiesCommitHash() == hash) {
            return
        }
        Timber.i("UtilitiesLoader: update utilities from bundle")
        updateUtilities()

        defaults.setLastUtilitiesCommitHash(hash)
    }

    private fun updateUtilities() {
        unzipper.unzipStream(
            zipInputStream = context.assets.open("utilities/utilities.zip"),
            location = fileStore.utilitiesDirectory().absolutePath
        )
    }

    private fun loadLastUtilitiesCommitHash(): String {
        return loadFromBundle(resource = "utilities/utilities_hash.txt", map = { it })
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
            Timber.e(error, "UtilitiesLoader: can't update from bundle")
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
            if (!fileStore.utilitiesDirectory().exists()) {
                if (type != UpdateType.initial) {
                    Timber.e("UtilitiesLoader: utilities directory was missing!")
                }
                fileStore.utilitiesDirectory().mkdirs()
            }

            if (type == UpdateType.initial) {
                return
            }

            val fileCount = fileStore.utilitiesDirectory().listFiles()?.size ?: 0

            if (fileCount != 0) {
                return
            }

            defaults.setLastTimestamp(0L)
            defaults.setLastUtilitiesCommitHash("")
        } catch (error: Exception) {
            Timber.e(error, "UtilitiesLoader: unable to restore folder integrity")
            throw error
        }

    }

    private fun process(error: Exception) {
        Timber.e(error, "UtilitiesLoader: error")

        val isBundleLoadingError = (error as? Error)?.isBundleLoadingError == true
        if (!isBundleLoadingError) {
            return
        }
    }
}
package org.zotero.android.locales

import android.content.Context
import org.zotero.android.architecture.Defaults
import org.zotero.android.files.FileStore
import org.zotero.android.helpers.FileHelper
import org.zotero.android.helpers.Unzipper
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CslLocalesLoader @Inject constructor(
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

    fun updateCslLocalesIfNeeded() {
        _update()
    }

    private fun _update() {
        val type: UpdateType =
            if (defaults.getLastTimestamp() == 0L) {
                UpdateType.initial
            } else {
                UpdateType.startup
            }

        Timber.i("CslLocalesLoader: update CSL locales")
        try {
            checkFolderIntegrity(type = type)
            updateFromBundle()
            defaults.setLastTimestamp(System.currentTimeMillis() / 1000)
        } catch (error: Exception) {
            process(error = error)
        }
    }

    private fun _updateFromBundle(forceUpdate: Boolean) {
        val hash = loadLastLocalesCommitHash()
        Timber.i("CslLocalesLoader: should update CSL locales from bundle, forceUpdate=$forceUpdate; oldHash=${defaults.getLastCslLocalesCommitHash()}; newHash=$hash")
        if (!forceUpdate && defaults.getLastCslLocalesCommitHash() == hash) {
            return
        }
        Timber.i("CslLocalesLoader: update CSL locales from bundle")
        updateLocales()

        defaults.setLastCslLocalesCommitHash(hash)
    }

    private fun updateLocales() {
        unzipper.unzipStream(
            zipInputStream = context.assets.open("cslLocales/cslLocales.zip"),
            location = fileStore.cslLocalesDirectory().absolutePath
        )
    }

    private fun loadLastLocalesCommitHash(): String {
        return loadFromBundle(resource = "cslLocales/commit_hash.txt", map = { it })
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
            Timber.e(error, "CslLocalesLoader: can't update from bundle")
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
            if (!fileStore.cslLocalesDirectory().exists()) {
                if (type != UpdateType.initial) {
                    Timber.e("CslLocalesLoader: CSL locales directory was missing!")
                }
                fileStore.cslLocalesDirectory().mkdirs()
            }

            if (type == UpdateType.initial) {
                return
            }

            val fileCount = fileStore.cslLocalesDirectory().listFiles()?.size ?: 0

            if (fileCount != 0) {
                return
            }

            defaults.setLastTimestamp(0L)
            defaults.setLastCslLocalesCommitHash("")
        } catch (error: Exception) {
            Timber.e(error, "CslLocalesLoader: unable to restore folder integrity")
            throw error
        }

    }

    private fun process(error: Exception) {
        Timber.e(error, "CslLocalesLoader: error")

        val isBundleLoadingError = (error as? Error)?.isBundleLoadingError == true
        if (!isBundleLoadingError) {
            return
        }
    }
}
package org.zotero.android.translator.loader

import android.content.Context
import org.zotero.android.architecture.Defaults
import org.zotero.android.files.FileStore
import org.zotero.android.helpers.FileHelper
import org.zotero.android.helpers.Unzipper
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranslationLoader @Inject constructor(
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

    fun updateTranslationIfNeeded() {
        _update()
    }

    private fun _update() {
        val type: UpdateType =
            if (defaults.getLastTimestamp() == 0L) {
                UpdateType.initial
            } else {
                UpdateType.startup
            }

        Timber.i("TranslationLoader: update translation JS")
        try {
            checkFolderIntegrity(type = type)
            updateFromBundle()
            defaults.setLastTimestamp(System.currentTimeMillis() / 1000)
        } catch (error: Exception) {
            process(error = error, updateType = type)
        }
    }

    private fun _updateTranslationFromBundle(forceUpdate: Boolean) {
        val hash = loadLastTranslationCommitHash()
        Timber.i("TranslationLoader: should update translation from bundle, forceUpdate=$forceUpdate; oldHash=${defaults.getLastTranslationCommitHash()}; newHash=$hash")
        if (!forceUpdate && defaults.getLastTranslationCommitHash() == hash) {
            return
        }
        Timber.i("TranslationLoader: update translation from bundle")
        updateTranslation()

        defaults.setLastTranslationCommitHash(hash)
    }

    private fun updateTranslation() {
        unzipper.unzipStream(
            zipInputStream = context.assets.open("translator.zip"),
            location = fileStore.translatorDirectory().absolutePath
        )
    }

    private fun loadLastTranslationCommitHash(): String {
        return loadFromBundle(resource = "translation_commit_hash.txt", map = { it })
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
            _updateTranslationFromBundle(forceUpdate = false)
            val timestamp = loadLastTimestamp()
            if (timestamp > defaults.getLastTimestamp()) {
                defaults.setLastTimestamp(timestamp)
                return
            } else {
                return
            }

        } catch (error: Exception) {
            Timber.e(error, "TranslatorsLoader: can't update from bundle")
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
            if (!fileStore.translatorDirectory().exists()) {
                if (type != UpdateType.initial) {
                    Timber.e("TranslationLoader: translation directory was missing!")
                }
                fileStore.translatorDirectory().mkdirs()
            }

            if (type == UpdateType.initial) {
                return
            }

            val fileCount = fileStore.translatorDirectory().listFiles()?.size ?: 0

            if (fileCount != 0) {
                return
            }

            defaults.setLastTimestamp(0L)
            defaults.setLastTranslationCommitHash("")
        } catch (error: Exception) {
            Timber.e(error, "TranslationLoader: unable to restore folder integrity")
            throw error
        }

    }

    private fun process(error: Exception, updateType: UpdateType) {
        Timber.e(error, "TranslatorsLoader: error")

        val isBundleLoadingError = (error as? Error)?.isBundleLoadingError == true
        if (!isBundleLoadingError) {
            return
        }
        //TODO show bundle load error dialog

    }
}
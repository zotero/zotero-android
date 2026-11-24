package org.zotero.android.loaders.noteeditor

import android.content.Context
import org.zotero.android.architecture.Defaults
import org.zotero.android.files.FileStore
import org.zotero.android.helpers.Unzipper
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteEditorLoader @Inject constructor(
    private val context: Context,
    private val defaults: Defaults,
    private val unzipper: Unzipper,
    private val fileStore: FileStore,
) {
    fun ensureInstalled(): Boolean {
        updateNoteEditorIfNeeded()
        return File(fileStore.noteEditorDirectory(), ENTRY_FILE).exists()
    }

    @Synchronized
    fun updateNoteEditorIfNeeded() {
        try {
            val bundledHash = loadBundledHash()
            val isInstalled = File(fileStore.noteEditorDirectory(), ENTRY_FILE).exists()
            if (isInstalled && defaults.getLastNoteEditorCommitHash() == bundledHash) {
                return
            }
            Timber.i("NoteEditorLoader: update note editor from bundle; oldHash=${defaults.getLastNoteEditorCommitHash()}; newHash=$bundledHash; wasInstalled=$isInstalled")
            install()
            defaults.setLastNoteEditorCommitHash(bundledHash)
        } catch (error: Exception) {
            Timber.e(error, "NoteEditorLoader: can't update note editor from bundle")
        }
    }

    private fun install() {
        val target = fileStore.noteEditorDirectory()
        val staging = File(target.parentFile, "${target.name}.staging")
        staging.deleteRecursively()
        context.assets.open(BUNDLED_ZIP).use { zipInputStream ->
            unzipper.unzipStream(zipInputStream = zipInputStream, location = staging.absolutePath)
        }
        if (!File(staging, ENTRY_FILE).exists()) {
            staging.deleteRecursively()
            throw IllegalStateException("$ENTRY_FILE missing in $BUNDLED_ZIP")
        }
        target.deleteRecursively()
        if (!staging.renameTo(target)) {
            staging.copyRecursively(target = target, overwrite = true)
            staging.deleteRecursively()
        }
    }

    private fun loadBundledHash(): String {
        return context.assets.open(BUNDLED_HASH).use { it.bufferedReader().readText() }.trim()
    }

    private companion object {
        const val BUNDLED_ZIP = "note-editor/note-editor.zip"
        const val BUNDLED_HASH = "note-editor/note_editor_hash.txt"
        const val ENTRY_FILE = "editor.html"
    }
}
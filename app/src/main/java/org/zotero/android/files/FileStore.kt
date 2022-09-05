package org.zotero.android.files

import android.content.Context
import org.zotero.android.architecture.GlobalVariables
import org.zotero.android.architecture.SdkPrefs
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Used to store objects and object trees in files.
 */
@Singleton
class FileStore @Inject constructor (
    private val context: Context,
    private val sdkPrefs: SdkPrefs,
    private val globalVariables: GlobalVariables,
) {

    private lateinit var rootDirectory: File

    private lateinit var dbFile: File
    private lateinit var bundledDataDbFile: File

    companion object {
    }

    fun init() {
        initializeDirectories()
        initDbFiles()
    }

    private fun initDbFiles() {
        val userId = sdkPrefs.getUserId()
        dbFile = fileURLForFilename("maindb_$userId.realm")!!
        bundledDataDbFile = fileURLForFilename("translators.realm")!!
    }

    private fun initializeDirectories() {
        val filesDir = context.filesDir
        rootDirectory = filesDir
        rootDirectory.mkdirs()
    }

    fun pathForFilename(filename: String?): String {
        return File(rootDirectory, filename).absolutePath
    }

    /**
     * Creates a java file object for a given filename.
     */
    fun fileURLForFilename(filename: String?): File? {
        return if (filename == null || filename.isEmpty()) {
            null
        } else File(pathForFilename(filename))
    }

    fun getDbFile() = dbFile
    fun getBundledDataDbFile() = bundledDataDbFile

    fun getRootDirectory() = rootDirectory

}

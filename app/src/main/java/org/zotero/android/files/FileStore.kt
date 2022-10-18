package org.zotero.android.files

import android.content.Context
import android.content.res.AssetManager
import com.google.common.base.Charsets
import com.google.common.io.Files
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.internal.closeQuietly
import org.zotero.android.architecture.GlobalVariables
import org.zotero.android.architecture.SdkPrefs
import org.zotero.android.sync.LibraryIdentifier
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Used to store objects and object trees in files.
 */
@Singleton
class FileStore @Inject constructor (
    private val context: Context,
    private val sdkPrefs: SdkPrefs,
    private val dataMarshaller: DataMarshaller,
    private val globalVariables: GlobalVariables,
) {

    private lateinit var rootDirectory: File

    private lateinit var dbFile: File
    private lateinit var bundledDataDbFile: File

    companion object {
        private const val BUNDLED_SCHEMA_FILE = "schema.json"
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


    @Throws(IOException::class)
    fun loadAssetIntoJsonObject(assetFileName: String): JsonObject {
        try {
            val assetManager: AssetManager = context.assets
            val assetFileDescriptor = assetManager.openFd(assetFileName)
            val inputStream = assetFileDescriptor.createInputStream()
            val fromJson = Gson().fromJson(InputStreamReader(inputStream), JsonObject::class.java)
            inputStream.closeQuietly()
            return fromJson
        } catch (e: IOException) {
            throw e
        }
    }

    fun getBundledSchema(): JsonObject? {
        return try {
            loadAssetIntoJsonObject(BUNDLED_SCHEMA_FILE)
        } catch (e: Exception) {
            Timber.d("Failed to load cached bundled data")
            null
        }
    }

    fun saveBundledSchema(data: JsonObject) {
        try {
            saveObject(data, BUNDLED_SCHEMA_FILE)
        } catch (e: Exception) {
            Timber.e(e, "Failed to cache bundled schema")
        }
    }

    /**
     * Serializes any object into json and stores it in a given file
     * @param objectToStore object that needs to be stored
     * @param filename name of the file to store json
     */
    fun saveObject(objectToStore: Any?, filename: String) {
        if (objectToStore == null) {
            return
        }
        val data = dataMarshaller.marshal(objectToStore)
        writeDataToFileWithName(filename, data)
    }

    private fun writeDataToFileWithName(filename: String, data: String) {
        try {
            Files.asCharSink(File(pathForFilename(filename)), Charsets.UTF_8).write(data)
        } catch (e: IOException) {
            Timber.e(e, "Unable to write data to file = $filename")
        }
    }

    fun attachmentFile(libraryId: LibraryIdentifier, key: String, filename: String, contentType: String): File {
        val name = split(filename = filename).first
        val folderPath = File(getRootDirectory(), "downloads/${libraryId.folderName}/$key")
        folderPath.mkdirs()
        val result = File(folderPath, name)
        return result
    }

    private fun split(filename: String): Pair<String, String> {
        val index = filename.lastIndexOf(".")
        if (index != -1) {
            val name = filename.substring(0, index)
            val ext = filename.substring(index + 1, filename.length)
            return name to ext
        }
        return filename to ""
    }

}

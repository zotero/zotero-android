package org.zotero.android.files

import android.content.Context
import android.content.res.AssetManager
import android.net.Uri
import com.google.common.base.Charsets
import com.google.common.io.Files
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.internal.closeQuietly
import org.zotero.android.androidx.content.getFileSize
import org.zotero.android.backgrounduploader.BackgroundUpload
import org.zotero.android.database.objects.RCustomLibraryType
import org.zotero.android.helpers.FileHelper
import org.zotero.android.helpers.MimeType
import org.zotero.android.sync.CollectionIdentifier
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.sync.SyncObject
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Used to store objects and object trees in files.
 */
@Singleton
class FileStore @Inject constructor (
    private val context: Context,
    private val dispatcher: CoroutineDispatcher,
    val dataMarshaller: DataMarshaller,
    private val gson: Gson
) {

    private lateinit var rootDirectory: File
    private lateinit var cachesDirectory: File
    private lateinit var debugDirectory: File
    private lateinit var crashDirectory: File
    private lateinit var readerDirtyPdfFolder: File
    private lateinit var exportHtmlFolder: File


    companion object {
        private const val file_store_version = 2


        private const val BUNDLED_SCHEMA_FILE = "schema.json"

        private const val ACTIVE_KEY_FILE = "backgroundUploadsFile"
        private const val SESSION_IDS_KEY_FILE = "activeUrlSessionIds"
        private const val EXTENSION_SESSION_IDS_KEY = "shareExtensionObservedUrlSessionIds"

        private const val selectedLibraryId = "selectedLibraryId_${file_store_version}_2.bin"
        private const val selectedCollectionId = "selectedCollectionId_${file_store_version}_2.bin"
    }

    fun init() {
        initializeDirectories()
    }

    fun dbFile(userId: Long): File {
        val dbDir = File(getRootDirectory(), "database")
        dbDir.mkdirs()
        return File(dbDir, "maindb_$userId.realm")
    }

    fun bundledDataDbFile(): File {
        val dbDir = File(getRootDirectory(), "database")
        dbDir.mkdirs()
        return File(dbDir, "translators.realm")
    }

    private fun initializeDirectories() {
        val filesDir = context.filesDir
        rootDirectory = filesDir
        rootDirectory.mkdirs()

        cachesDirectory = File(context.cacheDir, "Zotero")
        cachesDirectory.mkdirs()

        debugDirectory = File(filesDir, "debugLogging")
        debugDirectory.mkdirs()

        crashDirectory = File(filesDir, "crashLogging")
        crashDirectory.mkdirs()

        readerDirtyPdfFolder = File(filesDir, "readerDirtyPdf")
        readerDirtyPdfFolder.mkdirs()

        exportHtmlFolder = File(filesDir, "exportHtmlFolder")
        exportHtmlFolder.mkdirs()
    }

    fun pathForFilename(filename: String): String {
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

    fun getRootDirectory() = rootDirectory


    @Throws(IOException::class)
    fun loadAssetIntoJsonObject(assetFileName: String): JsonObject {
        try {
            val assetManager: AssetManager = context.assets
            val assetFileDescriptor = assetManager.openFd(assetFileName)
            val inputStream = assetFileDescriptor.createInputStream()
            val fromJson = gson.fromJson(InputStreamReader(inputStream), JsonObject::class.java)
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

    suspend fun attachmentFileAsync(
        libraryId: LibraryIdentifier,
        key: String,
        filename: String,
    ) = withContext(dispatcher) {
        attachmentFile(libraryId = libraryId, key = key, filename = filename)
    }

    fun attachmentFile(
        libraryId: LibraryIdentifier,
        key: String,
        filename: String,
    ): File {
        val folderPath = File(getRootDirectory(), "downloads/${libraryId.folderName}/$key")
        folderPath.mkdirs()
        val file = File(folderPath, filename)
        if (file.exists() && !file.isFile) {
            //We must ensure attachmentFile points to an actual file, not a folder
            file.delete()
        }
        return file
    }

    fun attachmentDirectory(libraryId: LibraryIdentifier, key: String): File {
        val folderPath = File(getRootDirectory(), "downloads/${libraryId.folderName}/$key")
        folderPath.mkdirs()
        return folderPath
    }

    fun annotationPreview(annotationKey: String, pdfKey: String, libraryId: LibraryIdentifier, isDark: Boolean): File {
        val folderPath = File(getRootDirectory(), "annotations/${libraryId.folderName}/$pdfKey")
        folderPath.mkdirs()
        val name = annotationKey + (if(isDark) "_dark" else "") + ".png"
        val result = File(folderPath, name)
        return result
    }

    fun exportHtmlFolder(): File {
        exportHtmlFolder.mkdirs()
        return exportHtmlFolder
    }

    fun exportHtmlFile(fileName: String): File {
        return File(exportHtmlFolder(), fileName)
    }

    fun generateTempFile(): File {
        return File(cache(), System.currentTimeMillis().toString())
    }

    fun temporaryFile(ext: String): File {
        return File(cache(), UUID.randomUUID().toString() + "." + ext)
    }

    fun annotationPreviews(pdfKey: String, libraryId: LibraryIdentifier): File {
        val folderPath = File(getRootDirectory(), "annotations/${libraryId.folderName}/$pdfKey")
        folderPath.mkdirs()
        return folderPath
    }

    fun annotationPreviews(libraryId: LibraryIdentifier): File {
        val folderPath = File(getRootDirectory(), "annotations/${libraryId.folderName}")
        folderPath.mkdirs()
        return folderPath
    }

    val annotationPreviews: File get() {
        val folderPath = File(getRootDirectory(), "annotations")
        folderPath.mkdirs()
        return folderPath
    }

    val pageThumbnails: File get() {
        val folderPath = File(getRootDirectory(), "thumbnails")
        folderPath.mkdirs()
        return folderPath
    }

    fun pageThumbnail(pageIndex: Int, key: String, libraryId: LibraryIdentifier, isDark: Boolean): File {
        val folderPath = File(getRootDirectory(), "thumbnails/${libraryId.folderName}/$key")
        folderPath.mkdirs()
        val name = pageIndex.toString() + (if(isDark) "_dark" else "") + ".png"
        val result = File(folderPath, name)
        return result
    }

    fun pageThumbnails(key: String, libraryId: LibraryIdentifier):File {
        val folderPath = File(getRootDirectory(), "thumbnails/${libraryId.folderName}/$key")
        folderPath.mkdirs()
        return folderPath
    }

    fun pageThumbnails(libraryId: LibraryIdentifier): File {
        val folderPath = File(getRootDirectory(), "thumbnails/${libraryId.folderName}")
        folderPath.mkdirs()
        return folderPath
    }

    fun jsonCacheFile(objectS: SyncObject, libraryId: LibraryIdentifier, key: String): File {
        val objectName: String = when (objectS) {
            SyncObject.collection ->
                "collection"

            SyncObject.item, SyncObject.trash ->
                "item"

            SyncObject.search ->
                "search"

            SyncObject.settings ->
                "settings"
        }
        val folderPath = File(getRootDirectory(), "jsons/${libraryId.folderName}/$objectName")
        folderPath.mkdirs()
        val result = File(folderPath, "$key.json")
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

    fun md5(file: File): String {
        val inputStream = FileInputStream(file)
        val md5 = FileHelper.md5(inputStream)
        return md5
    }

    fun getShareExtensionSessionIds(): List<String>? {
        return loadListDataWithFilename(
            EXTENSION_SESSION_IDS_KEY,
        )
    }

    fun saveShareExtensionSessions(identifiers: List<String>){
        saveObject(
            identifiers,
            EXTENSION_SESSION_IDS_KEY
        )
    }

    fun getSessionIds(): List<String>? {
        return loadListDataWithFilename(
            SESSION_IDS_KEY_FILE,
        )
    }

    fun saveSessions(identifiers: List<String>){
        saveObject(
            identifiers,
            SESSION_IDS_KEY_FILE
        )
    }

    fun deleteAllSessionIds(){
        deleteDataWithFilename(SESSION_IDS_KEY_FILE)
    }

    fun getUploads(): Map<Int, BackgroundUpload>? {
        return loadMapDataWithFilename(
            ACTIVE_KEY_FILE,
        )
    }

    fun saveUploads(uploads: Map<Int, BackgroundUpload>){
        saveObject(
            uploads,
            ACTIVE_KEY_FILE
        )
    }

    fun deleteAllUploads(){
        deleteDataWithFilename(ACTIVE_KEY_FILE)
    }


    internal inline fun <reified T> loadDataWithFilename(filename: String): T? {
        if (!fileExists(filename)) {
            return null
        }
        val path = pathForFilename(filename)
        return try {
            val json = Files.asCharSource(File(path), Charsets.UTF_8).read()
            dataMarshaller.unmarshal(json)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            null
        }
    }

    inline fun <reified T> loadListDataWithFilename(filename: String): MutableList<T>? {
        if (!fileExists(filename)) {
            return null
        }
        val path = pathForFilename(filename)
        return try {
            val json = Files.asCharSource(File(path), Charsets.UTF_8).read()
            dataMarshaller.unmarshalList(json)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            null
        }
    }

    inline fun <reified K, reified V> loadMapDataWithFilename(
        filename: String,
    ): Map<K, V>? {
        if (!fileExists(filename)) {
            return null
        }
        val path = pathForFilename(filename)
        return try {
            val json = Files.asCharSource(File(path), Charsets.UTF_8).read()
            dataMarshaller.unmarshalMap(json)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            null
        }
    }

    fun fileExists(filename: String): Boolean {
        return fileURLForFilename(filename)?.exists() ?: false
    }

    fun deleteDataWithFilename(filename: String?) {
        if (filename == null || !fileExists(filename)) {
            return
        }
        val file = fileURLForFilename(filename)
        file?.deleteRecursively()
    }

    fun isPdf(file: File): Boolean {
        val buffer = ByteArray(4)
        val inputStream: InputStream = FileInputStream(file)
        if (inputStream.read(buffer) != buffer.size) {
            // do something
        }
        inputStream.closeQuietly()
        return buffer.contentEquals(byteArrayOf(0x25, 0x50, 0x44, 0x46))
    }

    fun cache(): File {
        cachesDirectory.mkdirs()
        return cachesDirectory
    }

    fun debugLoggingDirectory(): File {
        this.debugDirectory.mkdirs()
        return this.debugDirectory
    }

    fun crashLoggingDirectory(): File {
        this.crashDirectory.mkdirs()
        return this.crashDirectory
    }

    fun getFileSize(uri: Uri): Long? = context.getFileSize(uri)

    fun openInputStream(uri: Uri): InputStream? =
        context.contentResolver.openInputStream(uri)

    fun getType(uri: Uri): MimeType? = context.contentResolver.getType(uri)

    val jsonCache: File get() {
        return File(getRootDirectory(), "jsons")
    }

    val uploads: File get() {
        return File(getRootDirectory(), "uploads")
    }

    val downloads: File get() {
        return File(getRootDirectory(), "downloads")
    }

    fun downloads(libraryId: LibraryIdentifier) : File {
        val folder = File(getRootDirectory(), "downloads")
        return File(folder, libraryId.folderName)
    }

    fun getSelectedLibrary(): LibraryIdentifier {
        val loadDataWithFilename = loadDataWithFilename<SelectedLibraryIdentifierWrapperForStorage?>(selectedLibraryId)
        return loadDataWithFilename?.libraryIdentifier
            ?: LibraryIdentifier.custom(RCustomLibraryType.myLibrary)
    }

    suspend fun getSelectedLibraryAsync() = withContext(dispatcher) {
        getSelectedLibrary()
    }

    suspend fun setSelectedLibraryAsync(
        libraryIdentifier: LibraryIdentifier,
    ) = withContext(dispatcher) {
        setSelectedLibrary(libraryIdentifier)
    }

    fun setSelectedLibrary(
        libraryIdentifier: LibraryIdentifier,
    ) {
        serializeToFile(selectedLibraryId, SelectedLibraryIdentifierWrapperForStorage(libraryIdentifier))
    }




    fun getSelectedCollectionId(): CollectionIdentifier {
        val loadDataWithFilename = loadDataWithFilename<SelectedCollectionIdentifierWrapperForStorage?>(selectedCollectionId)
        return loadDataWithFilename?.collectionIdentifier
            ?: CollectionIdentifier.custom(CollectionIdentifier.CustomType.all)
    }

    suspend fun getSelectedCollectionIdAsync() = withContext(dispatcher) {
        getSelectedCollectionId()
    }

    suspend fun setSelectedCollectionIdAsync(
        collectionIdentifier: CollectionIdentifier,
    ) = withContext(dispatcher) {
        setSelectedCollectionId(collectionIdentifier)
    }

    fun setSelectedCollectionId(
        collectionIdentifier: CollectionIdentifier,
    ) {
        serializeToFile(selectedCollectionId, SelectedCollectionIdentifierWrapperForStorage(collectionIdentifier))
    }

    private fun serializeToFile(fileName: String, objectToSave: Any) {
        saveObject(objectToSave, fileName)
    }

    fun shareExtensionDownload(key: String, ext: String): File {
        val folderPath = File(cache(), "shareext/downloads")
        folderPath.mkdirs()
        return File(folderPath, "item_$key.$ext")
    }

    fun translatorDirectory(): File {
        val folderPath = File(getRootDirectory(), "translator")
        folderPath.mkdirs()
        return folderPath
    }

    fun translatorItemsDirectory(): File {
        val folderPath = File(translatorDirectory(), "translator_items")
        folderPath.mkdirs()
        return folderPath
    }

    fun translator(filename: String): File {
        val name = split(filename = filename).first
        return File(translatorItemsDirectory(), name)
    }

    fun reset() {
        setSelectedCollectionId(CollectionIdentifier.custom(CollectionIdentifier.CustomType.all))
        setSelectedLibrary(LibraryIdentifier.custom(RCustomLibraryType.myLibrary))
    }

    fun temporaryZipUploadFile(key: String): File {
        val uploadsDir = File(getRootDirectory(), "uploads")
        uploadsDir.mkdirs()
        return File(uploadsDir, "${key}.zip")
    }

    fun pdfWorkerDirectory(): File {
        val folderPath = File(getRootDirectory(), "pdf-worker")
        folderPath.mkdirs()
        return folderPath
    }

    fun citationDirectory(): File {
        val folderPath = File(getRootDirectory(), "citation")
        folderPath.mkdirs()
        return folderPath
    }

    fun utilitiesDirectory(): File {
        val folderPath = File(citationDirectory(), "utilities")
        folderPath.mkdirs()
        return folderPath
    }

    fun readerDirtyPdfFolder(): File {
        readerDirtyPdfFolder.mkdirs()
        return readerDirtyPdfFolder
    }

    fun pdfReaderDirtyFile(fileName: String): File {
        return File(readerDirtyPdfFolder(), fileName)
    }

    fun stylesBundleExportDirectory(): File {
        val folderPath = File(getRootDirectory(), "stylesBundleExport")
        folderPath.mkdirs()
        return folderPath
    }

    fun stylesDirectory(): File {
        val folderPath = File(getRootDirectory(), "styles")
        folderPath.mkdirs()
        return folderPath
    }

    fun style(filenameWithoutExtension: String): File {
        return File(stylesDirectory(), "$filenameWithoutExtension.csl")
    }

    fun cslLocalesDirectory(): File {
        val folderPath = File(getRootDirectory(), "cslLocales")
        folderPath.mkdirs()
        return folderPath
    }

}

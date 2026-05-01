package org.zotero.android.screens.htmlepub.reader.sidebar

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.files.FileStore
import org.zotero.android.sync.LibraryIdentifier
import java.io.FileOutputStream
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HtmlEpubThumbnailPreviewManager @Inject constructor(
    private val dispatchers: Dispatchers,
    private val fileStore: FileStore,
    private val memoryCache: HtmlEpubThumbnailPreviewMemoryCache,
) {
    private val currentlyProcessingThumbnails = Collections.synchronizedSet(mutableSetOf<String>())
    private var coroutineScope = CoroutineScope(dispatchers.default)

    fun store(
        pageIndex: Int,
        key: String,
        encodedImageBase64String: String,
        libraryId: LibraryIdentifier,
        isDark: Boolean,
    ) {
        enqueue(
            pageIndex = pageIndex,
            key = key,
            encodedImageBase64String = encodedImageBase64String,
            libraryId = libraryId,
            isDark = isDark,
        )
    }

    fun delete(pageIndex: Int, key: String, libraryId: LibraryIdentifier) {
        fileStore.pageThumbnail(
            pageIndex = pageIndex,
            key = key,
            libraryId = libraryId,
            isDark = true
        ).delete()
        fileStore.pageThumbnail(
            pageIndex = pageIndex,
            key = key,
            libraryId = libraryId,
            isDark = false
        ).delete()
    }

    fun deleteAll(key: String, libraryId: LibraryIdentifier) {
        val pageThumbnails = fileStore.pageThumbnails(key = key, libraryId = libraryId)
        val list = pageThumbnails.list()
        println(list)
        pageThumbnails.deleteRecursively()
    }

    fun hasThumbnail(
        page: Int,
        key: String,
        libraryId: LibraryIdentifier,
        isDark: Boolean
    ): Boolean {
        return fileStore.pageThumbnail(
            pageIndex = page,
            key = key,
            libraryId = libraryId,
            isDark = isDark
        ).exists()
    }

    private fun enqueue(
        encodedImageBase64String: String,
        pageIndex: Int,
        key: String,
        libraryId: LibraryIdentifier,
        isDark: Boolean = false,
    ) = coroutineScope.launch {
        if (currentlyProcessingThumbnails.contains(key)) {
            return@launch
        }
        currentlyProcessingThumbnails.add(key)

        val cleanString = encodedImageBase64String.substringAfter(",")
        val decodedBytes = Base64.decode(cleanString, Base64.DEFAULT)
        val resultBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        
        completeRequest(
            bitmap = resultBitmap,
            key = key,
            pageIndex = pageIndex,
            libraryId = libraryId,
            isDark = isDark
        )
        currentlyProcessingThumbnails.remove(key)
    }

    private fun completeRequest(
        bitmap: Bitmap,
        key: String,
        libraryId: LibraryIdentifier,
        isDark: Boolean,
        pageIndex: Int,
    ) {
        cache(
            bitmap = bitmap,
            key = key,
            pageIndex = pageIndex,
            libraryId = libraryId,
            isDark = isDark
        )
        memoryCache.addToCache(pageIndex, bitmap)
    }

    private fun cache(
        bitmap: Bitmap,
        key: String,
        pageIndex: Int,
        libraryId: LibraryIdentifier,
        isDark: Boolean
    ) {
        val tempFile = fileStore.generateTempFile()
        val fileStream = FileOutputStream(tempFile)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileStream)
        fileStream.close()

        val finalFile = fileStore.pageThumbnail(
            key = key,
            pageIndex = pageIndex,
            libraryId = libraryId,
            isDark = isDark
        )
        tempFile.renameTo(finalFile)
    }

    fun cancelProcessing() {
        currentlyProcessingThumbnails.clear()
        coroutineScope.cancel()
        coroutineScope = CoroutineScope(dispatchers.default)
    }
}
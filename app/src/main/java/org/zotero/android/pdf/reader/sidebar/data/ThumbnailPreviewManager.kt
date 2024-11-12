package org.zotero.android.pdf.reader.sidebar.data

import android.content.Context
import android.graphics.Bitmap
import com.pspdfkit.configuration.rendering.PageRenderConfiguration
import com.pspdfkit.document.PdfDocument
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
class ThumbnailPreviewManager @Inject constructor(
    private val dispatchers: Dispatchers,
    private val fileStore: FileStore,
    private val memoryCache: ThumbnailPreviewMemoryCache,
    private val context: Context,
) {
    private val currentlyProcessingThumbnails = Collections.synchronizedSet(mutableSetOf<String>())
    private var coroutineScope = CoroutineScope(dispatchers.default)

    fun store(
        pageIndex: Int,
        key: String,
        document: PdfDocument,
        libraryId: LibraryIdentifier,
        isDark: Boolean,
    ) {
        enqueue(
            pageIndex = pageIndex,
            key = key,
            document = document,
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
        document: PdfDocument,
        pageIndex: Int,
        key: String,
        libraryId: LibraryIdentifier,
        isDark: Boolean = false,
    ) = coroutineScope.launch {
        if (currentlyProcessingThumbnails.contains(key)) {
            return@launch
        }
        currentlyProcessingThumbnails.add(key)

        val resultBitmap: Bitmap = generatePageBitmap(
            document = document,
            pageIndex = pageIndex,
            isDark = isDark
        )
        
        completeRequest(
            bitmap = resultBitmap,
            key = key,
            pageIndex = pageIndex,
            libraryId = libraryId,
            isDark = isDark
        )
        currentlyProcessingThumbnails.remove(key)
    }

    private fun generatePageBitmap(
        document: PdfDocument,
        pageIndex: Int,
        isDark: Boolean,
    ): Bitmap {
        val documentSize = document.getPageSize(pageIndex)
        val renderConfig = PageRenderConfiguration.Builder()
            .region(
                0,
                0,
                documentSize.width.toInt(),
                documentSize.height.toInt(),
            )
            .invertColors(isDark)
            .build()
        val pageBitmap: Bitmap = document.renderPageToBitmap(
            context,
            pageIndex,
            documentSize.width.toInt(),
            documentSize.height.toInt(),
            renderConfig
        )
        return pageBitmap
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
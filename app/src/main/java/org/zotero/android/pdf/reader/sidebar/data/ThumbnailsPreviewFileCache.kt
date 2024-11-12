package org.zotero.android.pdf.reader.sidebar.data

import android.graphics.BitmapFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.files.FileStore
import org.zotero.android.sync.LibraryIdentifier
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThumbnailsPreviewFileCache @Inject constructor(
    private val dispatchers: Dispatchers,
    private val fileStore: FileStore,
    private val thumbnailPreviewMemoryCache: ThumbnailPreviewMemoryCache,
) {
    private var coroutineScope = CoroutineScope(dispatchers.default)

    private val currentlyProcessingThumbnails = Collections.synchronizedSet(mutableSetOf<Int>())

    fun preview(key: String, pageIndex: Int, libraryId: LibraryIdentifier, isDark: Boolean) =
        coroutineScope.launch {
            val previewFile = fileStore.pageThumbnail(
                key = key,
                pageIndex = pageIndex,
                libraryId = libraryId,
                isDark = isDark
            )
            if (currentlyProcessingThumbnails.contains(pageIndex)) {
                return@launch
            }
            if (!previewFile.exists()) {
                return@launch
            }
            currentlyProcessingThumbnails.add(pageIndex)
            val decodedBitmap = BitmapFactory.decodeFile(previewFile.absolutePath) ?: return@launch
            thumbnailPreviewMemoryCache.addToCache(pageIndex, decodedBitmap)
            currentlyProcessingThumbnails.remove(pageIndex)
        }

    fun cancelProcessing() {
        currentlyProcessingThumbnails.clear()
        coroutineScope.cancel()
        coroutineScope = CoroutineScope(dispatchers.default)
    }

}
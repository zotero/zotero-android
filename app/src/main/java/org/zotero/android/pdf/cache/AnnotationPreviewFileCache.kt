package org.zotero.android.pdf.cache

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
class AnnotationPreviewFileCache @Inject constructor(
    private val dispatchers: Dispatchers,
    private val fileStore: FileStore,
    private val annotationPreviewMemoryCache: AnnotationPreviewMemoryCache,
) {
    private var coroutineScope = CoroutineScope(dispatchers.default)

    private val currentlyProcessingAnnotations = Collections.synchronizedSet(mutableSetOf<String>())

    fun preview(key: String, parentKey: String, libraryId: LibraryIdentifier, isDark: Boolean) =
        coroutineScope.launch {
            val previewFile = fileStore.annotationPreview(
                annotationKey = key,
                pdfKey = parentKey,
                libraryId = libraryId,
                isDark = isDark
            )
            if (currentlyProcessingAnnotations.contains(key)) {
                return@launch
            }
            if (!previewFile.exists()) {
                return@launch
            }
            currentlyProcessingAnnotations.add(key)
            val decodedBitmap = BitmapFactory.decodeFile(previewFile.absolutePath) ?: return@launch
            annotationPreviewMemoryCache.addToCache(key, decodedBitmap)
            currentlyProcessingAnnotations.remove(key)
        }

    fun cancelProcessing() {
        currentlyProcessingAnnotations.clear()
        coroutineScope.cancel()
        coroutineScope = CoroutineScope(dispatchers.default)
    }

}
package org.zotero.android.pdf.cache

import android.graphics.Bitmap
import android.util.LruCache
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@ViewModelScoped
class AnnotationPreviewMemoryCache @Inject constructor(
    private val eventStream: AnnotationPreviewCacheUpdatedEventStream
){
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 2
    private val memoryCache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    fun addToCache(key: String, bitmap: Bitmap) {
        memoryCache.put(key, bitmap)
        eventStream.emitAsync(key)
    }

    fun getBitmap(key: String): Bitmap? {
        return memoryCache.get(key)
    }
}
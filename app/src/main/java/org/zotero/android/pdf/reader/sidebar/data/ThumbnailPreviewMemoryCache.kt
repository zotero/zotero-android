package org.zotero.android.pdf.reader.sidebar.data

import android.graphics.Bitmap
import android.util.LruCache
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThumbnailPreviewMemoryCache @Inject constructor(
    private val eventStream: ThumbnailPreviewCacheUpdatedEventStream
){
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 2
    private val memoryCache: LruCache<Int, Bitmap> = object : LruCache<Int, Bitmap>(cacheSize) {
        override fun sizeOf(key: Int, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    fun addToCache(key: Int, bitmap: Bitmap) {
        memoryCache.put(key, bitmap)
        eventStream.emitAsync(key)
    }

    fun getBitmap(pageIndex: Int): Bitmap? {
        return memoryCache.get(pageIndex)
    }

    fun clear() {
        memoryCache.evictAll()
    }
}
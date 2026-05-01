package org.zotero.android.screens.htmlepub.reader.sidebar

import android.graphics.Bitmap
import android.util.LruCache
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HtmlEpubThumbnailPreviewMemoryCache @Inject constructor(
    private val eventStream: HtmlEpubThumbnailPreviewCacheUpdatedEventStream
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
package org.zotero.android.screens.htmlepub.reader.sidebar.thumbnails.cache

import android.graphics.Bitmap
import android.util.LruCache
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HtmlEpubThumbnailPreviewMemoryCache @Inject constructor(
){
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 2
    private val memoryCache: LruCache<Int, Bitmap> = object : LruCache<Int, Bitmap>(cacheSize) {
        override fun sizeOf(key: Int, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    fun addToCache(pagesToBitmapsList: List<Pair<Int, Bitmap>>): Map<Int, Bitmap> {
        pagesToBitmapsList.forEach {
            memoryCache.put(it.first, it.second)
        }
        return memoryCache.snapshot()
    }

    fun clear() {
        memoryCache.evictAll()
    }


}
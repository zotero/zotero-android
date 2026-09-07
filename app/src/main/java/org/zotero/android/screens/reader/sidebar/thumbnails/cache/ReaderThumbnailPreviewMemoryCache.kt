package org.zotero.android.screens.reader.sidebar.thumbnails.cache

import android.graphics.Bitmap
import android.util.LruCache
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@ViewModelScoped
class ReaderThumbnailPreviewMemoryCache @Inject constructor(
){
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    //TODO TEST ON LOW MEMORY!
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

    fun isInCache(index: Int): Boolean {
        return memoryCache.get(index) != null
    }

    fun clear() {
        memoryCache.evictAll()
    }


}
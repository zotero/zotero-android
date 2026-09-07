package org.zotero.android.screens.reader.sidebar.annotations.cache

import android.graphics.Bitmap
import android.util.LruCache
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@ViewModelScoped
class ReaderAnnotationBitmapMemoryCache @Inject constructor(
){
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 2
    private val memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    fun addToCache(pagesToBitmapsList: List<Pair<String, Bitmap>>): Map<String, Bitmap> {
        pagesToBitmapsList.forEach {
            memoryCache.put(it.first, it.second)
        }
        return memoryCache.snapshot()
    }

    fun isInCache(index: String): Boolean {
        return memoryCache.get(index) != null
    }

    fun clear() {
        memoryCache.evictAll()
    }


}
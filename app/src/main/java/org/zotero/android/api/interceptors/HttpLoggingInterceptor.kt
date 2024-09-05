package org.zotero.android.api.interceptors

import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level
import timber.log.Timber

object HttpLoggingInterceptor {
    fun createInterceptor(logLevel: Level): HttpLoggingInterceptor {
        return HttpLoggingInterceptor { message ->
            Timber.d(message)
        }.apply { level = logLevel }
    }
}

package org.zotero.android.ktx

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

fun OkHttpClient.Builder.setNetworkTimeout(seconds: Long) =
    connectTimeout(seconds, TimeUnit.SECONDS)
        .readTimeout(seconds, TimeUnit.SECONDS)
        .writeTimeout(seconds, TimeUnit.SECONDS)
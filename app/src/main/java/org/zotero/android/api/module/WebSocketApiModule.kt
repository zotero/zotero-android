package org.zotero.android.api.module

import dagger.Module
import dagger.Provides
import dagger.hilt.migration.DisableInstallInCheck
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor.Level
import org.zotero.android.api.annotations.ForWebSocketApi
import org.zotero.android.api.interceptors.HttpLoggingInterceptor
import org.zotero.android.api.interceptors.UserAgentHeaderNetworkInterceptor
import org.zotero.android.api.interceptors.ZoteroApiHeadersNetworkInterceptor
import org.zotero.android.ktx.setNetworkTimeout
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@DisableInstallInCheck
object WebSocketApiModule {

    @Provides
    @Singleton
    @ForWebSocketApi
    fun provideOkHttpClient(
        zoteroApiHeadersNetworkInterceptor: ZoteroApiHeadersNetworkInterceptor,
        userAgentHeaderNetworkInterceptor: UserAgentHeaderNetworkInterceptor,
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .setNetworkTimeout(30L)
            .pingInterval(5, TimeUnit.SECONDS)
            .addInterceptor(zoteroApiHeadersNetworkInterceptor)
            .addInterceptor(userAgentHeaderNetworkInterceptor)
            .addInterceptor(HttpLoggingInterceptor.createInterceptor(Level.BODY))
            .build()
    }
}
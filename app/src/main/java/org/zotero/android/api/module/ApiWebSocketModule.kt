package org.zotero.android.api.module

import dagger.Module
import dagger.Provides
import dagger.hilt.migration.DisableInstallInCheck
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor.Level
import org.zotero.android.api.HttpLoggingInterceptor
import org.zotero.android.api.NetworkConfiguration
import org.zotero.android.api.annotations.ForWebSocket
import org.zotero.android.ktx.setNetworkTimeout
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@DisableInstallInCheck
object ApiWebSocketModule {

    @Provides
    @Singleton
    @ForWebSocket
    fun provideSocketOkHttpClient(
        configuration: NetworkConfiguration
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor.createInterceptor(Level.BODY))
            .setNetworkTimeout(configuration.networkTimeout)
            .pingInterval(5, TimeUnit.SECONDS)
            .build()
    }
}
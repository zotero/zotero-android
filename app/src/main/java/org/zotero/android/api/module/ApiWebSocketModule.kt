package org.zotero.android.api.module

import dagger.Module
import dagger.Provides
import dagger.hilt.migration.DisableInstallInCheck
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.zotero.android.api.ForWebSocket
import org.zotero.android.api.NetworkConfiguration
import org.zotero.android.ktx.setNetworkTimeout
import timber.log.Timber
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
            .addInterceptor(
                HttpLoggingInterceptor { message ->
                    Timber.tag("SvcSocket")
                    Timber.d(message)
                }
                    .apply { level = HttpLoggingInterceptor.Level.BODY }
            ).setNetworkTimeout(configuration.networkTimeout)
            .pingInterval(5, TimeUnit.SECONDS)
            .build()
    }
}
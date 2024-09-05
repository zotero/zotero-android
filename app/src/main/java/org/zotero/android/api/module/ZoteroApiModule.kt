package org.zotero.android.api.module

import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.migration.DisableInstallInCheck
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor.Level
import org.zotero.android.BuildConfig
import org.zotero.android.api.ZoteroApi
import org.zotero.android.api.annotations.ForZoteroApi
import org.zotero.android.api.interceptors.HttpLoggingInterceptor
import org.zotero.android.api.interceptors.UserAgentHeaderNetworkInterceptor
import org.zotero.android.api.interceptors.ZoteroApiHeadersNetworkInterceptor
import org.zotero.android.api.interceptors.ZoteroAuthHeadersNetworkInterceptor
import org.zotero.android.ktx.setNetworkTimeout
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@DisableInstallInCheck
object ZoteroApiModule {

    @Provides
    @ForZoteroApi
    fun provideRetrofitBuilder(gson: Gson): Retrofit.Builder {
        val stringConverter = ScalarsConverterFactory.create()
        val gsonConverter = GsonConverterFactory.create(gson)
        return Retrofit.Builder()
            .addConverterFactory(stringConverter)
            .addConverterFactory(gsonConverter)
    }

    @Provides
    @Singleton
    @ForZoteroApi
    fun provideRetrofit(
        @ForZoteroApi builder: Retrofit.Builder,
        @ForZoteroApi okHttpClient: OkHttpClient
    ): Retrofit {
        return builder
            .baseUrl(BuildConfig.BASE_API_URL)
            .client(okHttpClient)
            .build()
    }

    @Provides
    @Singleton
    @ForZoteroApi
    fun provideOkHttpClient(
        zoteroApiHeadersNetworkInterceptor: ZoteroApiHeadersNetworkInterceptor,
        zoteroAuthHeadersNetworkInterceptor: ZoteroAuthHeadersNetworkInterceptor,
        userAgentHeaderNetworkInterceptor: UserAgentHeaderNetworkInterceptor,
    ): OkHttpClient {
        val connectionPool = ConnectionPool(
            maxIdleConnections = 10,
            keepAliveDuration = 5,
            timeUnit = TimeUnit.MINUTES
        )
        val dispatcher = Dispatcher()
        dispatcher.maxRequests = 30
        dispatcher.maxRequestsPerHost = 30

        return OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .connectionPool(connectionPool)
            .setNetworkTimeout(30L)
            .addInterceptor(zoteroApiHeadersNetworkInterceptor)
            .addInterceptor(userAgentHeaderNetworkInterceptor)
            .addInterceptor(zoteroAuthHeadersNetworkInterceptor)
            .addInterceptor(HttpLoggingInterceptor.createInterceptor(Level.BASIC))
            .build()
    }

    @Provides
    @Singleton
    fun provideZoteroApi(@ForZoteroApi retrofit: Retrofit): ZoteroApi =
        retrofit.create(ZoteroApi::class.java)

}
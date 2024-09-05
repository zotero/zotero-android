package org.zotero.android.api.module

import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.migration.DisableInstallInCheck
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor.Level
import org.zotero.android.api.NonZoteroApi
import org.zotero.android.api.annotations.ForNonZoteroApi
import org.zotero.android.api.interceptors.HttpLoggingInterceptor
import org.zotero.android.api.interceptors.UserAgentHeaderNetworkInterceptor
import org.zotero.android.ktx.setNetworkTimeout
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@DisableInstallInCheck
object NonZoteroApiModule {

    @Provides
    @ForNonZoteroApi
    fun provideRetrofitBuilder(gson: Gson): Retrofit.Builder {
        val stringConverter = ScalarsConverterFactory.create()
        val gsonConverter = GsonConverterFactory.create(gson)
        return Retrofit.Builder()
            .addConverterFactory(stringConverter)
            .addConverterFactory(gsonConverter)
    }

    @Provides
    @Singleton
    @ForNonZoteroApi
    fun provideRetrofit(
        @ForNonZoteroApi builder: Retrofit.Builder,
        @ForNonZoteroApi okHttpClient: OkHttpClient
    ): Retrofit {
        return builder
            .baseUrl("https://dummyurl.com") //no-op as all URLs for non-zotero API are absolute
            .client(okHttpClient)
            .build()
    }

    @Provides
    @Singleton
    @ForNonZoteroApi
    fun provideOkHttpClient(
        userAgentHeaderNetworkInterceptor: UserAgentHeaderNetworkInterceptor
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
            .addInterceptor(userAgentHeaderNetworkInterceptor)
            .addInterceptor(HttpLoggingInterceptor.createInterceptor(Level.BASIC))
            .build()
    }


    @Provides
    @Singleton
    fun provideNonZoteroApi(@ForNonZoteroApi retrofit: Retrofit): NonZoteroApi {
        return retrofit.create(NonZoteroApi::class.java)
    }

}
package org.zotero.android.api.module

import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.migration.DisableInstallInCheck
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor.Level
import org.zotero.android.api.NonZoteroNoRedirectApi
import org.zotero.android.api.annotations.ForNonZoteroNoRedirectsApi
import org.zotero.android.api.interceptors.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import javax.inject.Singleton

@Module
@DisableInstallInCheck
object NonZoteroNoRedirectModule {

    @Provides
    @ForNonZoteroNoRedirectsApi
    fun provideRetrofitBuilder(gson: Gson): Retrofit.Builder {
        val stringConverter = ScalarsConverterFactory.create()
        val gsonConverter = GsonConverterFactory.create(gson)
        return Retrofit.Builder()
            .addConverterFactory(stringConverter)
            .addConverterFactory(gsonConverter)
    }

    @Provides
    @Singleton
    @ForNonZoteroNoRedirectsApi
    fun provideRetrofit(
        @ForNonZoteroNoRedirectsApi builder: Retrofit.Builder,
        @ForNonZoteroNoRedirectsApi okHttpClient: OkHttpClient
    ): Retrofit {
        return builder
            .baseUrl("https://dummyurl.com") //no-op as all URLs for non-zotero API are absolute
            .client(okHttpClient)
            .build()
    }

    @Provides
    @Singleton
    @ForNonZoteroNoRedirectsApi
    fun provideOkHttpClient(
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .followRedirects(false)
            .addInterceptor(HttpLoggingInterceptor.createInterceptor(Level.BASIC))
            .build()
    }

    @Provides
    @Singleton
    fun provideNonZoteroNoRedirectApi(@ForNonZoteroNoRedirectsApi retrofit: Retrofit): NonZoteroNoRedirectApi =
        retrofit.create(NonZoteroNoRedirectApi::class.java)

}

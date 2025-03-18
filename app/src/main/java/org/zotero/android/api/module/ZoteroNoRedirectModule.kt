package org.zotero.android.api.module

import dagger.Module
import dagger.Provides
import dagger.hilt.migration.DisableInstallInCheck
import okhttp3.OkHttpClient
import org.zotero.android.BuildConfig
import org.zotero.android.api.ZoteroNoRedirectApi
import org.zotero.android.api.annotations.ForZoteroApi
import org.zotero.android.api.annotations.ForZoteroNoRedirectsApi
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@DisableInstallInCheck
object ZoteroNoRedirectModule {

    @Provides
    @Singleton
    @ForZoteroNoRedirectsApi
    fun provideRetrofit(
        @ForZoteroApi baseBuilder: Retrofit.Builder,
        @ForZoteroNoRedirectsApi okHttpClient: OkHttpClient
    ): Retrofit {
        return baseBuilder
            .baseUrl(BuildConfig.BASE_API_URL)
            .client(okHttpClient)
            .build()
    }

    @Provides
    @Singleton
    @ForZoteroNoRedirectsApi
    fun provideOkHttpClient(
        @ForZoteroApi baseClient: OkHttpClient,
    ): OkHttpClient {
        return baseClient
            .newBuilder()
            .followRedirects(false)
            .build()
    }

    @Provides
    @Singleton
    fun provideZoteroNoRedirectApi(@ForZoteroNoRedirectsApi retrofit: Retrofit): ZoteroNoRedirectApi =
        retrofit.create(ZoteroNoRedirectApi::class.java)

}

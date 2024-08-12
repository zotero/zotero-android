package org.zotero.android.api.module

import dagger.Module
import dagger.Provides
import dagger.hilt.migration.DisableInstallInCheck
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor.Level
import org.zotero.android.BuildConfig
import org.zotero.android.api.AuthNetworkInterceptor
import org.zotero.android.api.HttpLoggingInterceptor
import org.zotero.android.api.annotations.ForApiWithNoRedirects
import org.zotero.android.api.annotations.ForBaseApi
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@DisableInstallInCheck
object ApiNoRedirectsModule {

    @Provides
    @Singleton
    @ForApiWithNoRedirects
    fun provideOkHttpWithNoRedirects(
        @ForBaseApi baseClient: OkHttpClient,
        authInterceptor: AuthNetworkInterceptor,
    ): OkHttpClient {
        return baseClient
            .newBuilder()
            .followRedirects(false)
            .addInterceptor(authInterceptor)
            .addInterceptor(HttpLoggingInterceptor.createInterceptor(Level.BASIC))
            .build()
    }

    @Provides
    @Singleton
    @ForApiWithNoRedirects
    fun provideRetrofitWithNoRedirectsBuilder(
        @ForBaseApi baseBuilder: Retrofit.Builder,
        @ForApiWithNoRedirects okHttpClient: OkHttpClient
    ): Retrofit.Builder {
        return baseBuilder.client(okHttpClient)
    }

    @Provides
    @Singleton
    @ForApiWithNoRedirects
    fun provideRetrofitWithAuthentication(
        @ForApiWithNoRedirects baseBuilder: Retrofit.Builder,
        @ForApiWithNoRedirects okHttpClient: OkHttpClient
    ): Retrofit {
        return baseBuilder
            .baseUrl(BuildConfig.BASE_API_URL)
            .client(okHttpClient)
            .build()
    }

}

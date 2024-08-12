package org.zotero.android.api.module

import dagger.Module
import dagger.Provides
import dagger.hilt.migration.DisableInstallInCheck
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor.Level
import org.zotero.android.BuildConfig
import org.zotero.android.api.AuthNetworkInterceptor
import org.zotero.android.api.HttpLoggingInterceptor
import org.zotero.android.api.annotations.ForApiWithAuthentication
import org.zotero.android.api.annotations.ForBaseApi
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@DisableInstallInCheck
object ApiWithAuthenticationModule {

    @Provides
    @Singleton
    @ForApiWithAuthentication
    fun provideRetrofitWithAuthenticationBuilder(
        @ForBaseApi baseBuilder: Retrofit.Builder,
        @ForApiWithAuthentication okHttpClient: OkHttpClient
    ): Retrofit.Builder {
        return baseBuilder.client(okHttpClient)
    }

    @Provides
    @Singleton
    @ForApiWithAuthentication
    fun provideRetrofitWithAuthentication(
        @ForApiWithAuthentication baseBuilder: Retrofit.Builder,
        @ForApiWithAuthentication okHttpClient: OkHttpClient
    ): Retrofit {
        return baseBuilder
            .baseUrl(BuildConfig.BASE_API_URL)
            .client(okHttpClient)
            .build()
    }

    @Provides
    @Singleton
    @ForApiWithAuthentication
    fun provideOkHttpWithAuthentication(
        @ForBaseApi baseClient: OkHttpClient,
        authInterceptor: AuthNetworkInterceptor,
    ): OkHttpClient {
        return baseClient
            .newBuilder()
            .addInterceptor(authInterceptor)
            .addInterceptor(HttpLoggingInterceptor.createInterceptor(Level.BASIC))
         .build()
    }
}
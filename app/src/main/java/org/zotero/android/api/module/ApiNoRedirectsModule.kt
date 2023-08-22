package org.zotero.android.api.module

import dagger.Module
import dagger.Provides
import dagger.hilt.migration.DisableInstallInCheck
import okhttp3.OkHttpClient
import org.zotero.android.BuildConfig
import org.zotero.android.api.AuthNetworkInterceptor
import org.zotero.android.api.ForApiWithNoRedirects
import org.zotero.android.api.ForBaseApi
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
        val clientBuilder = baseClient.newBuilder()
        clientBuilder.followRedirects(false)
        clientBuilder.addInterceptor(authInterceptor)
        return clientBuilder.build()
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

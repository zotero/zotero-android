package org.zotero.android.api.module

import dagger.Module
import dagger.Provides
import dagger.hilt.migration.DisableInstallInCheck
import org.zotero.android.api.AccountApi
import org.zotero.android.api.ForApiWithAuthentication
import org.zotero.android.api.ForApiWithNoRedirects
import org.zotero.android.api.ForBaseApi
import org.zotero.android.api.NoAuthenticationApi
import org.zotero.android.api.NoRedirectApi
import org.zotero.android.api.SyncApi
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@DisableInstallInCheck
object ApiInterfacesModule {

    @Provides
    @Singleton
    fun provideNoRedirectApi(@ForApiWithNoRedirects retrofit: Retrofit) =
        retrofit.create(NoRedirectApi::class.java)

    @Provides
    @Singleton
    fun provideAccountApi(@ForApiWithAuthentication retrofit: Retrofit) =
        retrofit.create(AccountApi::class.java)

    @Provides
    @Singleton
    fun provideSyncApi(@ForApiWithAuthentication retrofit: Retrofit) =
        retrofit.create(SyncApi::class.java)

    @Provides
    @Singleton
    fun provideNoAuthenticationApi(@ForBaseApi retrofit: Retrofit): NoAuthenticationApi {
        return retrofit.create(NoAuthenticationApi::class.java)
    }
}
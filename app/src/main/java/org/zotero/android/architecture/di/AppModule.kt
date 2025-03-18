package org.zotero.android.architecture.di

import android.app.Application
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.SupervisorJob
import org.zotero.android.api.module.NonZoteroApiModule
import org.zotero.android.api.module.NonZoteroNoRedirectModule
import org.zotero.android.api.module.WebSocketApiModule
import org.zotero.android.api.module.ZoteroApiModule
import org.zotero.android.api.module.ZoteroNoRedirectModule
import org.zotero.android.architecture.coroutines.ApplicationScope
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.architecture.coroutines.QDispatchers
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module(
    includes = [
        JsonModule::class,
        ZoteroApiModule::class,
        ZoteroNoRedirectModule::class,
        NonZoteroApiModule::class,
        NonZoteroNoRedirectModule::class,
        WebSocketApiModule::class,
    ]
)
internal class AppModule {

    @Provides
    fun provideDispatchers(): Dispatchers = QDispatchers()

    @Provides
    fun provideIoCoroutineDispatcher(dispatchers: Dispatchers): CoroutineDispatcher = dispatchers.io

    @Provides
    fun provideContext(app: Application): Context = app

    @Provides
    @Singleton
    fun provideAppScope() = ApplicationScope(SupervisorJob())
}

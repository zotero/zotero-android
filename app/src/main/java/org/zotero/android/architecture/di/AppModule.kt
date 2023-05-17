package org.zotero.android.architecture.di

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.media.AudioManager
import android.os.Build
import androidx.core.content.getSystemService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.SupervisorJob
import org.zotero.android.BuildConfig
import org.zotero.android.api.ApiModule
import org.zotero.android.architecture.SdkInt
import org.zotero.android.architecture.app.AppConfig
import org.zotero.android.architecture.app.ApplicationIdProvider
import org.zotero.android.architecture.app.BuildConfiguration
import org.zotero.android.architecture.coroutines.ApplicationScope
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.architecture.coroutines.QDispatchers
import org.zotero.android.architecture.ui.LoadingIndicatorProvider
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module(
    includes = [ApiModule::class]
)
internal class AppModule {
    @Provides
    fun provideDispatchers(): Dispatchers = QDispatchers()

    @Provides
    fun provideIoCoroutineDispatcher(dispatchers: Dispatchers): CoroutineDispatcher = dispatchers.io

    @Provides
    fun provideSdkInt(): SdkInt = SdkInt(Build.VERSION.SDK_INT)

    @Provides
    fun provideAppConfig(application: Application): AppConfig {
        val packageInfo = application.packageManager.getPackageInfo(application.packageName, 0)
        return AppConfig(packageInfo.versionName)
    }

    @Provides
    @Singleton
    fun provideApplicationIdProvider(): ApplicationIdProvider {
        return object : ApplicationIdProvider {
            override fun provide() = BuildConfig.APPLICATION_ID
        }
    }

    @Provides
    fun provideNotificationManager(app: Application): NotificationManager {
        return app.getSystemService()!!
    }

    @Provides
    fun provideContext(app: Application): Context = app

    @Provides
    fun provideLoadingIndicator() = LoadingIndicatorProvider()

    @Provides
    fun provideBuildConfiguration() = BuildConfiguration(BuildConfig.DEBUG)

    @Provides
    @Singleton
    fun provideAppScope() = ApplicationScope(SupervisorJob())

    @Provides
    fun provideAudioManager(context: Application) =
        context.getSystemService(AUDIO_SERVICE) as AudioManager
}

package org.zotero.android.framework

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import org.zotero.android.BuildConfig
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
open class ZoteroApplication : Configuration.Provider, Application() {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory


    companion object {
        lateinit var instance: ZoteroApplication
    }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        instance = this
    }

    override fun getWorkManagerConfiguration() =
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

}
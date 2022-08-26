package org.zotero.android.framework

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
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
        instance = this
    }

    override fun getWorkManagerConfiguration() =
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

}
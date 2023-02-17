package org.zotero.android

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.Job
import org.zotero.android.architecture.coroutines.ApplicationScope
import org.zotero.android.files.FileStore
import org.zotero.android.sync.Controllers
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
open class ZoteroApplication : Configuration.Provider, Application(), DefaultLifecycleObserver {

    private lateinit var job: Job

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var controllers: Controllers

    @Inject
    lateinit var applicationScope: ApplicationScope

    @Inject
    lateinit var fileStore: FileStore

    companion object {
        lateinit var instance: ZoteroApplication
    }

    override fun onCreate() {
        super<Application>.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        instance = this

        controllers.init()

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun getWorkManagerConfiguration() =
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onStart(owner: LifecycleOwner) {
        controllers.willEnterForeground()
    }

    override fun onStop(owner: LifecycleOwner) {
        controllers.didEnterBackground()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        controllers.willTerminate()
    }
}
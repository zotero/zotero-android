package org.zotero.android

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import org.zotero.android.architecture.coroutines.ApplicationScope
import org.zotero.android.architecture.crashreporting.CrashReportingTree
import org.zotero.android.architecture.logging.DebugLoggingTree
import org.zotero.android.files.FileStore
import org.zotero.android.sync.Controllers
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
open class ZoteroApplication : Configuration.Provider, Application(), DefaultLifecycleObserver {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var controllers: Controllers

    @Inject
    lateinit var applicationScope: ApplicationScope

    @Inject
    lateinit var fileStore: FileStore

    @Inject
    lateinit var debugLoggingTree: DebugLoggingTree

    companion object {
        lateinit var instance: ZoteroApplication
    }

    override fun onCreate() {
        super<Application>.onCreate()
        instance = this
        setUpLogging()

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

    private fun setUpLogging() {
        Timber.plant(CrashReportingTree(), this.debugLoggingTree)
    }
}
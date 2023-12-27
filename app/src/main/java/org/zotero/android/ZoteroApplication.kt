package org.zotero.android

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.gson.Gson
import dagger.hilt.android.HiltAndroidApp
import org.zotero.android.BuildConfig.EVENT_AND_CRASH_LOGGING_ENABLED
import org.zotero.android.api.ForGsonWithRoundedDecimals
import org.zotero.android.architecture.coroutines.ApplicationScope
import org.zotero.android.architecture.crashreporting.FirebaseCrashReportingTree
import org.zotero.android.architecture.logging.crash.CrashFileWriter
import org.zotero.android.architecture.logging.debug.DebugLoggingTree
import org.zotero.android.files.FileStore
import org.zotero.android.sync.Controllers
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
open class ZoteroApplication: Application(), DefaultLifecycleObserver {

    @Inject
    lateinit var controllers: Controllers

    @Inject
    lateinit var applicationScope: ApplicationScope

    @Inject
    lateinit var fileStore: FileStore

    @Inject
    lateinit var debugLoggingTree: DebugLoggingTree

    @Inject
    lateinit var gson: Gson

    @Inject
    lateinit var crashFileWriter: CrashFileWriter

    @Inject
    @ForGsonWithRoundedDecimals
    lateinit var gsonWithRoundedDecimals: Gson

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
        if (!EVENT_AND_CRASH_LOGGING_ENABLED) {
            Timber.plant(object : Timber.DebugTree() {
                override fun createStackElementTag(element: StackTraceElement): String {
                    return "[${Thread.currentThread().name}]" +
                            "${super.createStackElementTag(element)}"
                }
            })
            return
        }
        val defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            crashFileWriter.writeCrashToFile(throwable.stackTraceToString())
            defaultExceptionHandler?.uncaughtException(
                thread,
                throwable
            )
        }
        Timber.plant(FirebaseCrashReportingTree(), this.debugLoggingTree)
    }
}
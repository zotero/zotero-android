package org.zotero.android

import android.app.Application
import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.gson.Gson
import com.pspdfkit.PSPDFKit
import dagger.hilt.android.HiltAndroidApp
import org.zotero.android.BuildConfig.EVENT_AND_CRASH_LOGGING_ENABLED
import org.zotero.android.androidx.content.longToast
import org.zotero.android.api.annotations.ForGsonWithRoundedDecimals
import org.zotero.android.architecture.Defaults
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
    lateinit var defaults: Defaults

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
        initializePspdfKit()
    }

    private fun initializePspdfKit() {
        val initializationResult = attemptToInitializePspdfKit(this)
        defaults.setPspdfkitInitialized(initializationResult)

    }

    private fun attemptToInitializePspdfKit(context: Context): Boolean {
        try {
            if (BuildConfig.PSPDFKIT_KEY.isNotBlank()) {
                PSPDFKit.initialize(context, BuildConfig.PSPDFKIT_KEY)
            } else {
                PSPDFKit.initialize(context, null)
            }
        } catch (e: Exception) {
            Timber.e(e, "Unable to initialize PSPDFKIT")
            context.longToast("Unable to initialize PSPDFKIT")
            return false
        }
        return true
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
        val consoleDebugTree = object : Timber.DebugTree() {
            override fun createStackElementTag(element: StackTraceElement): String {
                return "[${Thread.currentThread().name}]" +
                        "${super.createStackElementTag(element)}"
            }
        }
        val listOfTrees = mutableListOf(consoleDebugTree, debugLoggingTree)
        if (EVENT_AND_CRASH_LOGGING_ENABLED) {
            val defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                crashFileWriter.writeCrashToFile(throwable.stackTraceToString())
                defaultExceptionHandler?.uncaughtException(
                    thread,
                    throwable
                )
            }
            listOfTrees.add(FirebaseCrashReportingTree())
        }
        Timber.plant(*listOfTrees.toTypedArray())
    }
}
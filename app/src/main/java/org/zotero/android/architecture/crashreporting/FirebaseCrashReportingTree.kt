package org.zotero.android.architecture.crashreporting

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

class FirebaseCrashReportingTree : Timber.DebugTree() {
    private var isTesting: Boolean? = null

    override fun createStackElementTag(element: StackTraceElement): String {
        return "[${Thread.currentThread().name}]" +
                "${super.createStackElementTag(element)}"
    }

    override fun log(
        priority: Int,
        tag: String?,
        message: String,
        t: Throwable?
    ) {
        super.log(priority, tag, message, t)
        if (isTesting() || priority == Log.VERBOSE || priority == Log.DEBUG || priority == Log.INFO) {
            return
        }
        if (t != null) {
            FirebaseCrashlytics.getInstance().log(message)
            FirebaseCrashlytics.getInstance().recordException(t)
        } else {
            FirebaseCrashlytics.getInstance().recordException(Exception(message))
        }
    }

    private fun isTesting(): Boolean {
        if (isTesting == null) {
            isTesting = try {
                Class.forName("org.junit.Test")
                true
            } catch (e: ClassNotFoundException) {
                false
            }
        }
        return isTesting!!
    }
}

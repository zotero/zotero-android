package org.zotero.android.architecture.logging

import android.os.Build
import org.zotero.android.BuildConfig

object DeviceInfoProvider {
    val crashString: String get() {
        return "device => $device, os => $osVersion, version => $versionAndBuild"
    }

    val debugString: String get() {
        return "Version: $versionAndBuild\nDevice: $device\nOS: $osVersion"
    }

    val versionAndBuild: String get() {
        return "$versionString ($buildString)"
    }

    val versionString: String get() {
        return BuildConfig.VERSION_NAME
    }

    val buildString: String get() {
        return BuildConfig.VERSION_CODE.toString()
    }

    val device: String get() {
        return "${Build.MANUFACTURER} ${Build.DEVICE}"
    }

    val osVersion: String get() {
        return "Android "+ Build.VERSION.SDK_INT
    }

}
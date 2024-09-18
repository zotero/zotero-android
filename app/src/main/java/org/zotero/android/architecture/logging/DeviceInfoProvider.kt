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

    private val versionAndBuild: String get() {
        val devBuildIndicator = if (BuildConfig.FLAVOR == "dev") {
            "dev"
        } else ""
        return "$versionString ($buildString) $devBuildIndicator"
    }

    private val versionString: String get() {
        return BuildConfig.VERSION_NAME
    }

    private val buildString: String get() {
        return BuildConfig.VERSION_CODE.toString()
    }

    private val device: String get() {
        return "${Build.MANUFACTURER} ${Build.DEVICE}"
    }

    private val osVersion: String get() {
        return "Android "+ Build.VERSION.SDK_INT
    }

    val userAgentString: String get() {
        return "Zotero/${BuildConfig.VERSION_NAME} (Android ${Build.VERSION.SDK_INT})"
    }
}
package org.zotero.android.ktx

import android.os.Build
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

fun AppCompatActivity.enableEdgeToEdgeAndTranslucency() {
    enableEdgeToEdge()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        window.setNavigationBarContrastEnforced(false)
    }
}
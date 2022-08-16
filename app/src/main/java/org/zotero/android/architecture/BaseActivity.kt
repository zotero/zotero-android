package org.zotero.android.architecture

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.zotero.android.BuildConfig

abstract class BaseActivity : AppCompatActivity() {

    protected open val lockOrientationPortrait: Boolean = true

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Lock orientation in portrait mode for release builds only
        if (lockOrientationPortrait) {
            requestedOrientation = if (BuildConfig.DEBUG) {
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            } else {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
    }
}

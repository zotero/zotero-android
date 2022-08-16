package org.zotero.android.sample.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import org.zotero.android.architecture.BaseActivity

@AndroidEntryPoint
internal class SampleActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SampleScreen(onBack = { finish() })
        }
    }

}


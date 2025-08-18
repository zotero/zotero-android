package org.zotero.android.sample.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import org.zotero.android.architecture.BaseActivity
import org.zotero.android.uicomponents.theme.CustomTheme

@AndroidEntryPoint
internal class SampleActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CustomTheme {
                SampleScreen(onBack = { finish() })
            }
        }
    }

}


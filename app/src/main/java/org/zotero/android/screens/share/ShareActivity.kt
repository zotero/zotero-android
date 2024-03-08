package org.zotero.android.screens.share


import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import org.zotero.android.architecture.BaseActivity
import org.zotero.android.uicomponents.theme.CustomTheme

@AndroidEntryPoint
internal class ShareActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CustomTheme {
                ShareScreen(onBack = { finish() })
            }
        }
    }

    companion object {
        fun getIntent(
            extraIntent: Intent,
            context: Context,
        ): Intent {
            return Intent(context, ShareActivity::class.java).apply {
                putExtras(extraIntent)
            }
        }
    }

}
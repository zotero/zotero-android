package org.zotero.android.pdf

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import org.zotero.android.architecture.BaseActivity
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.uicomponents.theme.CustomTheme

@AndroidEntryPoint
internal class PdfReaderActivity: BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val params = ScreenArguments.pdfReaderArgs
        setContent {
            CustomTheme {
                PdfReaderScreen(uri = params.uri, onBack = { finish() })
            }
        }
    }

    companion object {
        fun getIntent(
            context: Context,
        ): Intent {
            return Intent(context, PdfReaderActivity::class.java).apply {
            }
        }
    }
}


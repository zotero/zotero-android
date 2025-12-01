package org.zotero.android.screens.share


import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import org.zotero.android.architecture.BaseActivity
import org.zotero.android.ktx.enableEdgeToEdgeAndTranslucency
import org.zotero.android.screens.share.navigation.ShareRootNavigation
import org.zotero.android.uicomponents.themem3.AppThemeM3
import javax.inject.Inject

@AndroidEntryPoint
internal class ShareActivity : BaseActivity() {
    @Inject
    lateinit var shareRawAttachmentLoader: ShareRawAttachmentLoader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdgeAndTranslucency()
        shareRawAttachmentLoader.loadFromIntent(intent)
        setContent {
            AppThemeM3 {
                ShareRootNavigation()
            }
        }
    }

    companion object {
        fun getIntent(
            extraIntent: Intent,
            context: Context,
        ): Intent {
            return Intent(context, ShareActivity::class.java).apply {
                data = extraIntent.data
                putExtras(extraIntent)
            }
        }
    }

}
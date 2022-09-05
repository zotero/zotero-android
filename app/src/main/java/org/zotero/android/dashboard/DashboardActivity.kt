package org.zotero.android.dashboard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import org.zotero.android.architecture.BaseActivity
import org.zotero.android.dashboard.ui.DashboardScreen
import org.zotero.android.uicomponents.theme.CustomTheme

@AndroidEntryPoint
internal class DashboardActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CustomTheme {
                DashboardScreen(
                    onBack = { finish() },
                )
            }
        }
    }

    companion object {
        fun getIntentClearTask(
            context: Context,
        ): Intent {
            return Intent(context, DashboardActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
        }

        fun getIntent(
            context: Context,
        ): Intent {
            return Intent(context, DashboardActivity::class.java).apply {
            }
        }
    }
}


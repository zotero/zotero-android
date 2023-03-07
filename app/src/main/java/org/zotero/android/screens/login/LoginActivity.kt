package org.zotero.android.screens.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import org.zotero.android.architecture.BaseActivity
import org.zotero.android.screens.dashboard.DashboardActivity
import org.zotero.android.uicomponents.theme.CustomTheme

@AndroidEntryPoint
internal class LoginActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CustomTheme {
                LoginScreen(
                    onBack = { finish() },
                    navigateToDashboard = {
                        startActivity(DashboardActivity.getIntentClearTask(this))
                    }
                )
            }
        }
    }

    companion object {
        fun getIntent(
            context: Context,
        ): Intent {
            return Intent(context, LoginActivity::class.java).apply {
            }
        }
    }
}


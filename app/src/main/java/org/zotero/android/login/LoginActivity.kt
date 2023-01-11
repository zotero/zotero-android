package org.zotero.android.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import dagger.hilt.android.AndroidEntryPoint
import org.zotero.android.architecture.BaseActivity
import org.zotero.android.dashboard.DashboardActivity
import org.zotero.android.login.ui.LoginScreen
import org.zotero.android.uicomponents.theme.CustomTheme

@ExperimentalAnimationApi
@AndroidEntryPoint
internal class LoginActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CustomTheme {
                LoginScreen(
                    onBack = { finish() },
                    navigateToDashboard = { startActivity(DashboardActivity.getIntentClearTask(this)) }
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


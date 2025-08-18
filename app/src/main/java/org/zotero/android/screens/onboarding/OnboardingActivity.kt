package org.zotero.android.screens.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import org.zotero.android.architecture.BaseActivity
import org.zotero.android.screens.login.LoginActivity
import org.zotero.android.uicomponents.theme.CustomTheme

@AndroidEntryPoint
internal class OnboardingActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CustomTheme {
                OnboardingScreen(
                    onSignInClick = {
                        startActivity(LoginActivity.getIntent(this))
                    },
                )
            }
        }
    }

    companion object {
        fun getIntent(
            context: Context,
        ): Intent {
            return Intent(context, OnboardingActivity::class.java).apply {
            }
        }
    }


}


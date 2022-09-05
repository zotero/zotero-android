package org.zotero.android.onboarding.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import org.zotero.android.architecture.BaseActivity
import org.zotero.android.login.LoginActivity
import org.zotero.android.uicomponents.theme.CustomTheme

@AndroidEntryPoint
internal class OnboardingActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CustomTheme {
                OnboardingScreen(
                    onBack = { finish() },
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


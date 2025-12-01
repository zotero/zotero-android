package org.zotero.android.screens.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import org.zotero.android.architecture.BaseActivity
import org.zotero.android.ktx.enableEdgeToEdgeAndTranslucency
import org.zotero.android.screens.login.LoginActivity
import org.zotero.android.uicomponents.themem3.AppThemeM3

@AndroidEntryPoint
internal class OnboardingActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdgeAndTranslucency()

        setContent {
            AppThemeM3 {
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


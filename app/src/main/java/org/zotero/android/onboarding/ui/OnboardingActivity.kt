package org.zotero.android.onboarding.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import org.zotero.android.architecture.BaseActivity
import org.zotero.android.uicomponents.theme.CustomTheme

@AndroidEntryPoint
internal class OnboardingActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CustomTheme {
                OnboardingScreen(
                    onBack = { finish() },
                    onSignUpClick = {
                    },
                )
            }
        }
    }

}


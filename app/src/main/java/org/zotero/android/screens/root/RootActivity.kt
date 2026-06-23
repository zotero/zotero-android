package org.zotero.android.screens.root

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import org.zotero.android.architecture.BaseActivity
import org.zotero.android.architecture.Screen
import org.zotero.android.ktx.enableEdgeToEdgeAndTranslucency
import org.zotero.android.screens.dashboard.DashboardActivity
import org.zotero.android.screens.onboarding.OnboardingActivity
import org.zotero.android.screens.share.ShareActivity
import org.zotero.android.uicomponents.loading.CircularLoading
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.themem3.AppThemeM3

@AndroidEntryPoint
class RootActivity : BaseActivity(), Screen<RootViewState, RootViewEffect> {
    private val viewModel: RootViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdgeAndTranslucency()

        viewModel.init(this.intent)
        viewModel.observeViewChanges(this)

        setContent {
            AppThemeM3 {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .background(CustomTheme.colors.surface)) {
                    Box(modifier = Modifier.align(Alignment.Center)) {
                        CircularLoading()
                    }
                }
            }
        }
    }

    override fun render(state: RootViewState) = Unit

    override fun trigger(effect: RootViewEffect) = when (effect) {
        RootViewEffect.NavigateToSignIn -> navigateToOnboarding()
        RootViewEffect.NavigateToDashboard -> navigateToDashboard()
        RootViewEffect.NavigateToShare -> navigateToShare()
    }

    private fun navigateToOnboarding() {
        val intent: Intent = OnboardingActivity.getIntent(this)
        startActivity(intent)
        finish()
    }

    private fun navigateToDashboard() {
        val intent: Intent = DashboardActivity.getIntent(this)
        startActivity(intent)
        finish()
    }

    private fun navigateToShare() {
        val intent: Intent = ShareActivity.getIntent(
            extraIntent = this.intent,
            context = this
        )
        startActivity(intent)
        finish()
    }

    companion object {
        fun getIntentClearTask(
            context: Context,
        ): Intent {
            return Intent(context, RootActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
        }
    }
}

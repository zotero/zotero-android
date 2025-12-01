package org.zotero.android.screens.root

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.zotero.android.architecture.BaseActivity
import org.zotero.android.architecture.Screen
import org.zotero.android.ktx.enableEdgeToEdgeAndTranslucency
import org.zotero.android.screens.dashboard.DashboardActivity
import org.zotero.android.screens.onboarding.OnboardingActivity
import org.zotero.android.screens.share.ShareActivity

@AndroidEntryPoint
class RootActivity : BaseActivity(), Screen<RootViewState, RootViewEffect> {
    private val viewModel: RootViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdgeAndTranslucency()

        viewModel.init(this.intent)
        viewModel.observeViewChanges(this)
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

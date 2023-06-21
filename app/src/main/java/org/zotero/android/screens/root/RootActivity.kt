package org.zotero.android.screens.root

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.zotero.android.architecture.BaseActivity
import org.zotero.android.architecture.Screen
import org.zotero.android.screens.dashboard.DashboardActivity
import org.zotero.android.screens.onboarding.OnboardingActivity

@AndroidEntryPoint
class RootActivity : BaseActivity(), Screen<RootViewState, RootViewEffect> {
    private val viewModel: RootViewModel by viewModels()

    /*
    On Android Oreo the app crashes if we're trying to request the orientation for
    translucent activity. Although translucent activity always inherits the
    orientation from the underlying activity. So it's safe to set this parameter
    to false for all transparent activities.
     */
    override val lockOrientationPortrait = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.init()
        viewModel.observeViewChanges(this)
    }

    override fun render(state: RootViewState) = Unit

    override fun trigger(effect: RootViewEffect) = when (effect) {
        RootViewEffect.NavigateToSignIn -> navigateToOnboarding()
        RootViewEffect.NavigateToDashboard -> navigateToDashboard()
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

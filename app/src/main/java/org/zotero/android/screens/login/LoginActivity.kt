package org.zotero.android.screens.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import dagger.hilt.android.AndroidEntryPoint
import org.zotero.android.androidx.content.longErrorSnackbar
import org.zotero.android.androidx.content.showKeyboard
import org.zotero.android.architecture.BaseActivity
import org.zotero.android.architecture.Screen
import org.zotero.android.databinding.LoginActivityBinding
import org.zotero.android.screens.dashboard.DashboardActivity
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.snackbar.SnackbarMessage

@AndroidEntryPoint
internal class LoginActivity : BaseActivity(), Screen<LoginViewState, LoginViewEffect> {

    private lateinit var binding: LoginActivityBinding

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = LoginActivityBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        viewModel.observeViewChanges(this)


        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val innerPadding = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
                        or WindowInsetsCompat.Type.displayCutout()
                        or WindowInsetsCompat.Type.ime()
            )
            v.setPadding(
                innerPadding.left,
                innerPadding.top,
                innerPadding.right,
                innerPadding.bottom
            )
            insets
        }

        binding.signInButton.setOnClickListener {
            viewModel.onUsernameChanged(binding.usernameEditText.text.toString())
            viewModel.onPasswordChanged(binding.passwordEditText.text.toString())
            viewModel.onSignInClicked()
        }
        binding.cancelButton.setOnClickListener {
            finish()
        }
        binding.forgotPasswordButton.setOnClickListener {
            val intent = Intent(
                Intent.ACTION_VIEW,
                "https://www.zotero.org/user/lostpassword?app=1".toUri()
            )
            startActivity(intent)
        }
        showKeyboard(binding.usernameEditText)
    }

    companion object {
        fun getIntent(
            context: Context,
        ): Intent {
            return Intent(context, LoginActivity::class.java).apply {
            }
        }
    }

    override fun render(state: LoginViewState) {
        val snackbarMessage = state.snackbarMessage
        if (snackbarMessage != null && snackbarMessage is SnackbarMessage.ErrorMessageString) {
            binding.root.longErrorSnackbar(snackbarMessage.message)
            viewModel.dismissSnackbar()
        }
        if (state.isLoading) {
            binding.signInButton.text = ""
            binding.progressIndicator.visibility = View.VISIBLE
        } else {
            binding.signInButton.text = getString(Strings.onboarding_sign_in)
            binding.progressIndicator.visibility = View.GONE
        }
    }

    override fun trigger(effect: LoginViewEffect) = when (effect) {
        LoginViewEffect.NavigateBack -> finish()
        LoginViewEffect.NavigateToDashboard -> {
            startActivity(DashboardActivity.getIntentClearTask(this))
        }
    }
}


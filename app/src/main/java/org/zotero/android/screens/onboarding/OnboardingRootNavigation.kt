package org.zotero.android.screens.onboarding

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.zotero.android.architecture.navigation.ZoteroNavigation
import org.zotero.android.architecture.navigation.dialogDynamicHeight
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.screens.login.LoginScreen
import org.zotero.android.uicomponents.navigation.ZoteroNavHost

@Composable
internal fun OnboardingRootNavigation(
    navigateToDashboard: () -> Unit,
) {
    val navController = rememberNavController()
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val navigation = remember(navController) {
        ZoteroNavigation(navController, dispatcher)
    }
    Column {
        OnboardingRootNavHost(
            navController = navController,
            navigation = navigation,
            navigateToDashboard = navigateToDashboard
        )
    }

}

@Composable
private fun OnboardingRootNavHost(
    navController: NavHostController,
    navigation: ZoteroNavigation,
    navigateToDashboard: () -> Unit,
) {
    val layoutType = CustomLayoutSize.calculateLayoutType()

    ZoteroNavHost(
        navController = navController,
        startDestination = OnboardingRootDestinations.ONBOARDING_SCREEN,
    ) {

        val onSignInClick: () -> Unit = {
            if (layoutType.isTablet()) {
                navigation.toLoginDialog()
            } else {
                navigation.toLoginScreen()
            }
        }

        onboardingScreen(
            onSignInClick = onSignInClick,
        )
        if (layoutType.isTablet()) {
            loginDialog(onBack = navController::popBackStack, navigateToDashboard = navigateToDashboard)
        } else {
            loginScreen(onBack = navigation::onBack, navigateToDashboard = navigateToDashboard)
        }
    }
}

private fun NavGraphBuilder.onboardingScreen(
    onSignInClick: () -> Unit,
) {
    composable(
        route = OnboardingRootDestinations.ONBOARDING_SCREEN,
        arguments = listOf(),
    ) {
        OnboardingScreen(
            onSignInClick = onSignInClick,
        )
    }
}

private fun NavGraphBuilder.loginScreen(
    onBack: () -> Unit,
    navigateToDashboard: () -> Unit,
) {
    composable(
        route = OnboardingRootDestinations.LOGIN_SCREEN,
        arguments = listOf(),
    ) {
        LoginScreen(onBack = onBack, navigateToDashboard = navigateToDashboard)
    }
}

private fun NavGraphBuilder.loginDialog(
    onBack: () -> Unit,
    navigateToDashboard: () -> Unit,
) {
    dialogDynamicHeight(
        route = OnboardingRootDestinations.LOGIN_DIALOG,
    ) {
        LoginScreen(onBack = onBack, navigateToDashboard = navigateToDashboard)
    }
}

private fun ZoteroNavigation.toLoginScreen() {
    navController.navigate(OnboardingRootDestinations.LOGIN_SCREEN)
}

private fun ZoteroNavigation.toLoginDialog() {
    navController.navigate(OnboardingRootDestinations.LOGIN_DIALOG)
}

private object OnboardingRootDestinations {
    const val ONBOARDING_SCREEN = "onboardingScreen"
    const val LOGIN_SCREEN = "loginScreen"
    const val LOGIN_DIALOG = "loginDialog"
}


package org.zotero.android.screens.reader.readaloud

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import org.zotero.android.architecture.navigation.ZoteroNavigation
import org.zotero.android.architecture.navigation.dialogFixedDimens

private object ReadAloudDestinations {
    const val ONBOARDING = "readAloudOnboarding"
}

internal fun NavGraphBuilder.readAloudOnboardingDialog(navigation: ZoteroNavigation) {
    dialogFixedDimens(
        modifier = Modifier
            .height(640.dp)
            .width(420.dp),
        route = ReadAloudDestinations.ONBOARDING,
    ) {
        ReadAloudOnboardingScreen(onBack = navigation.navController::popBackStack)
    }
}

internal fun NavGraphBuilder.readAloudOnboardingScreen(navigation: ZoteroNavigation) {
    composable(route = ReadAloudDestinations.ONBOARDING) {
        ReadAloudOnboardingScreen(onBack = navigation.navController::popBackStack)
    }
}

internal fun ZoteroNavigation.toReadAloudOnboarding() {
    navController.navigate(ReadAloudDestinations.ONBOARDING)
}
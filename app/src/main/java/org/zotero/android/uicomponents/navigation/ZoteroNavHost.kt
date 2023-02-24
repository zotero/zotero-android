package org.zotero.android.uicomponents.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import com.google.accompanist.navigation.animation.AnimatedNavHost

@Composable
fun ZoteroNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.Center,
    route: String? = null,
    enterTransition: (AnimatedContentScope<NavBackStackEntry>.() -> EnterTransition) =
        { slideInHorizontally(initialOffsetX = { it }) },
    exitTransition: (AnimatedContentScope<NavBackStackEntry>.() -> ExitTransition) =
        { slideOutHorizontally(targetOffsetX = { -it }) },
    popEnterTransition: (AnimatedContentScope<NavBackStackEntry>.() -> EnterTransition) =
        { slideInHorizontally(initialOffsetX = { -it }) },
    popExitTransition: (AnimatedContentScope<NavBackStackEntry>.() -> ExitTransition) =
        { slideOutHorizontally(targetOffsetX = { it }) },
    builder: NavGraphBuilder.() -> Unit
) {
    AnimatedNavHost(
        builder = builder,
        contentAlignment = contentAlignment,
        enterTransition = enterTransition,
        exitTransition = exitTransition,
        modifier = modifier,
        navController = navController,
        popEnterTransition = popEnterTransition,
        popExitTransition = popExitTransition,
        route = route,
        startDestination = startDestination,
    )
}

package org.zotero.android.architecture.ui

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog
import com.google.accompanist.navigation.animation.composable

fun NavGraphBuilder.screenOrDialogFixedMaxHeight(
    layoutType: CustomLayoutSize.LayoutType,
    route: String,
    content: @Composable () -> Unit,
) {
    screenOrDialog(
        layoutType = layoutType,
        route = route,
        dialogModifier = Modifier.requiredHeightIn(max = 400.dp),
        content = content
    )
}

fun NavGraphBuilder.screenOrDialogFixedDimens(
    modifier:Modifier,
    layoutType: CustomLayoutSize.LayoutType,
    route: String,
    content: @Composable () -> Unit,
) {
    screenOrDialog(
        layoutType = layoutType,
        route = route,
        dialogModifier = modifier,
        content = content
    )
}

fun NavGraphBuilder.screenOrDialogDynamicHeight(
    layoutType: CustomLayoutSize.LayoutType,
    route: String,
    enterTransition: (AnimatedContentScope<NavBackStackEntry>.() -> EnterTransition?)? = null,
    content: @Composable () -> Unit,
) {
    screenOrDialog(
        layoutType = layoutType,
        route = route,
        dialogModifier = Modifier.fillMaxHeight(0.8f),
        enterTransition = enterTransition,
        content = content
    )
}

private fun NavGraphBuilder.screenOrDialog(
    layoutType: CustomLayoutSize.LayoutType,
    route: String,
    dialogModifier: Modifier,
    enterTransition: (AnimatedContentScope<NavBackStackEntry>.() -> EnterTransition?)? = null,
    content: @Composable () -> Unit,
) {
    when (layoutType.showScreenOrDialog()) {
        CustomLayoutSize.ScreenOrDialogToShow.DIALOG -> {
            customDialog(route = route, dialogModifier = dialogModifier) {
                content()
            }
        }
        CustomLayoutSize.ScreenOrDialogToShow.SCREEN -> {
            composable(
                route = route,
                arguments = listOf(),
                enterTransition = enterTransition,
            ) {
                content()
            }
        }
    }
}

fun NavGraphBuilder.customDialog(
    route: String,
    dialogModifier: Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    dialog(
        route = route,
        dialogProperties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        )
    ) {
        Box(
            modifier = dialogModifier
                .clip(RoundedCornerShape(16.dp))
        ) {
            content()
        }
    }
}
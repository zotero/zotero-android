package org.zotero.android.architecture.navigation

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog
import org.zotero.android.uicomponents.theme.CustomTheme

fun NavGraphBuilder.dialogFixedDimens(
    modifier:Modifier,
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    content: @Composable () -> Unit,
) {
    customDialog(
        route = route,
        dialogModifier = modifier,
        arguments = arguments,
        content = content
    )
}

fun NavGraphBuilder.dialogFixedMaxHeight(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    content: @Composable () -> Unit,
) {
    customDialog(
        route = route,
        arguments = arguments,
        dialogModifier = Modifier.requiredHeightIn(max = 400.dp),
        content = content
    )
}

fun NavGraphBuilder.dialogDynamicHeight(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    content: @Composable () -> Unit,
) {
    customDialog(
        route = route,
        arguments = arguments,
        dialogModifier = Modifier.fillMaxHeight(0.8f),
        content = content
    )
}

private fun NavGraphBuilder.customDialog(
    route: String,
    dialogModifier: Modifier,
    arguments: List<NamedNavArgument> = emptyList(),
    content: @Composable () -> Unit,
) {
    dialog(
        route = route,
        arguments = arguments,
        dialogProperties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        )
    ) {
        Box(
            modifier = dialogModifier
                .clip(shape = RoundedCornerShape(16.dp))
                .border(
                    width = 1.dp,
                    color = CustomTheme.colors.dialogBorderColor,
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            content()
        }
    }
}
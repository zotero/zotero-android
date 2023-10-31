package org.zotero.android.architecture.navigation.toolbar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun BoxScope.SyncToolbarScreen(
    viewModel: SyncToolbarViewModel = hiltViewModel(),
) {
    val viewState by viewModel.viewStates.observeAsState(SyncToolbarViewState())
    val viewEffect by viewModel.viewEffects.observeAsState()
    LaunchedEffect(key1 = viewModel) {
        viewModel.init()
    }

    LaunchedEffect(key1 = viewEffect) {
        when (viewEffect?.consume()) {
//            NavigateBack -> onBack()
            null -> Unit
            else -> {}
        }
    }
    val layoutType = CustomLayoutSize.calculateLayoutType()

    val syncProgress = viewState.progress
    AnimatedContent(
        modifier = Modifier.align(Alignment.BottomStart),
        targetState = syncProgress != null,
        transitionSpec = {
            toolbarTransitionSpec()
        },
        label = ""
    ) { shouldShow ->
        if (shouldShow && syncProgress != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(layoutType.calculateSyncProgressBottomPanelHeight())
                    .background(color = CustomTheme.colors.surface)
                    .safeClickable(
                        onClick = viewModel::showErrorDialog,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    )
            ) {
                Text(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(start = 12.dp, end = 12.dp),
                    text = syncToolbarText(syncProgress, viewModel),
                    color = CustomTheme.colors.primaryContent,
                    style = CustomTheme.typography.default,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = layoutType.calculateTextSize(),
                )
            }
        }
    }
    SyncToolbarDialogs(viewState = viewState, viewModel = viewModel)
}

private fun AnimatedContentScope<Boolean>.toolbarTransitionSpec(): ContentTransform {
    val intOffsetSpec = tween<IntOffset>()
    return (slideInVertically(intOffsetSpec) { it } with
            slideOutVertically(intOffsetSpec) { it }).using(
        // Disable clipping since the faded slide-in/out should
        // be displayed out of bounds.
        SizeTransform(
            clip = false,
            sizeAnimationSpec = { _, _ -> tween() }
        ))
}





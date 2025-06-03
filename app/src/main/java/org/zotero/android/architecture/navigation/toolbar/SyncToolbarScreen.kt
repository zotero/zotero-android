package org.zotero.android.architecture.navigation.toolbar

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.architecture.navigation.toolbar.data.CurrentSyncProgressState
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.topbar.NewHeadingTextButton

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

    val syncProgress = viewState.progressState
    AnimatedContent(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = layoutType.calculateAllItemsBottomPanelHeight() + 16.dp),
        targetState = syncProgress != null,
        label = ""
    ) { shouldShow ->
        if (shouldShow && syncProgress != null) {
            val roundCornerShape = RoundedCornerShape(size = 10.dp)
            val widthFraction = if (layoutType.isTablet()) {
                0.7f
            } else {
                0.9f
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth(widthFraction)
                    .height(48.dp)
                    .background(color = Color(0xFF2E3138), roundCornerShape)
                    .clip(roundCornerShape)
                    .safeClickable(
                        onClick = viewModel::showErrorDialog,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ),
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 56.dp)
                        .align(Alignment.CenterStart),
                    text = syncProgress.message,
                    color = Color(0xFFF0F2F7),
                    style = CustomTheme.typography.newInfo,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp)
                ) {
                    when (syncProgress) {
                        is CurrentSyncProgressState.SyncFinishedWithError -> {
                            NewHeadingTextButton(
                                onClick = viewModel::showErrorDialog,
                                text = stringResource(Strings.sync_snackbar_view_action),
                            )
                        }

                        is CurrentSyncProgressState.Aborted -> {
                            NewHeadingTextButton(
                                onClick = viewModel::showErrorDialog,
                                text = stringResource(Strings.sync_snackbar_view_action),
                            )
                        }
                    }
                }

            }
        }
    }
    SyncToolbarDialogs(viewState = viewState, viewModel = viewModel)
}




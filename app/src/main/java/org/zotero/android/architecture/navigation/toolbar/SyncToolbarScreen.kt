package org.zotero.android.architecture.navigation.toolbar

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.architecture.navigation.toolbar.data.CurrentSyncProgressState
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.safeClickable
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
            null -> Unit
            else -> {}
        }
    }
    val layoutType = CustomLayoutSize.calculateLayoutType()

    val syncProgress = viewState.progressState
    AnimatedContent(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .windowInsetsPadding(BottomAppBarDefaults.windowInsets)
            .padding(bottom = BottomAppBarDefaults.FlexibleBottomAppBarHeight),
        targetState = syncProgress != null,
        label = ""
    ) { shouldShow ->
        if (shouldShow && syncProgress != null) {
            val roundCornerShape = RoundedCornerShape(size = 4.dp)
            val widthFraction = if (layoutType.isTablet()) {
                0.7f
            } else {
                0.9f
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth(widthFraction)
                    .height(68.dp)
                    .background(
                        color = MaterialTheme.colorScheme.inverseSurface,
                        shape = roundCornerShape
                    )
                    .clip(roundCornerShape)
                    .padding(16.dp)
                    .safeClickable(
                        onClick = viewModel::showErrorDialog,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = syncProgress.message,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                when (syncProgress) {
                    is CurrentSyncProgressState.SyncFinishedWithError, is CurrentSyncProgressState.Aborted -> {
                        NewHeadingTextButton(
                            onClick = viewModel::showErrorDialog,
                            text = stringResource(Strings.sync_snackbar_view_action),
                        )
                    }

                    else -> {
                        //no-op
                    }
                }

            }
        }
    }
    SyncToolbarDialogs(viewState = viewState, viewModel = viewModel)
}




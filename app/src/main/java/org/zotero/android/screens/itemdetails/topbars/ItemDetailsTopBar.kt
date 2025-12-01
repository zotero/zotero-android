package org.zotero.android.screens.itemdetails.topbars

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults.rememberTooltipPositionProvider
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings

@Composable
internal fun ItemDetailsTopBar(
    onViewOrEditClicked: () -> Unit,
    onCancelOrBackClicked: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    TopAppBar(
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        title = {
            Text(
                text = "",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge
            )
        },
        navigationIcon = {
            IconButton(onClick = onCancelOrBackClicked) {
                Icon(
                    painter = painterResource(Drawables.arrow_back_24dp),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        actions = {
            TooltipBox(
                positionProvider = rememberTooltipPositionProvider(
                    TooltipAnchorPosition.Below,
                    4.dp
                ),
                tooltip = {
                    PlainTooltip() {
                        Text(
                            stringResource(
                                Strings.edit
                            )
                        )
                    }
                },
                state = rememberTooltipState()
            ) {
                IconButton(onClick = onViewOrEditClicked) {
                    Icon(
                        painter = painterResource(Drawables.edit_24px),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }

            }
        },
    )
}

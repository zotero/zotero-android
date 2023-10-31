package org.zotero.android.screens.collections

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.topbar.HeadingTextButton

@Composable
internal fun CollectionsTopBar(
    viewState: CollectionsViewState,
    viewModel: CollectionsViewModel
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = viewState.library.name,
                color = CustomTheme.colors.primaryContent,
                style = CustomTheme.typography.h2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            Row {
                Spacer(modifier = Modifier.width(12.dp))
                HeadingTextButton(
                    isEnabled = true,
                    onClick = viewModel::navigateToLibraries,
                    text = stringResource(id = Strings.libs)
                )
                Spacer(modifier = Modifier.width(45.dp))
            }
        },
        actions = {
            Icon(
                modifier = Modifier.safeClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(bounded = false),
                    onClick = viewModel::onAdd
                ),
                painter = painterResource(id = Drawables.baseline_add_24),
                contentDescription = null,
                tint = CustomTheme.colors.dynamicTheme.primaryColor,
            )
            Spacer(modifier = Modifier.width(12.dp))
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = CustomTheme.colors.surface),
    )
}

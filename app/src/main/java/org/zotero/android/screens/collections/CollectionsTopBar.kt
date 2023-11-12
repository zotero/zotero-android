package org.zotero.android.screens.collections

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.icon.IconWithPadding
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
            IconWithPadding(drawableRes = Drawables.add_24px, onClick = viewModel::onAdd)
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = CustomTheme.colors.surface),
    )
}

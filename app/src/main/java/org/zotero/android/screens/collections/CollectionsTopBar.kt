package org.zotero.android.screens.collections

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.icon.IconWithPadding
import org.zotero.android.uicomponents.topbar.NewCustomTopBar
import org.zotero.android.uicomponents.topbar.NewHeadingTextButton

@Composable
internal fun CollectionsTopBar(
    viewState: CollectionsViewState,
    viewModel: CollectionsViewModel
) {
    NewCustomTopBar(
        title = viewState.library.name,
        leftContainerContent = listOf {
            NewHeadingTextButton(
                onClick = viewModel::navigateToLibraries,
                text = stringResource(id = Strings.libs)
            )
        },
        rightContainerContent = listOf {
            IconWithPadding(drawableRes = Drawables.add_24px, onClick = viewModel::onAdd)
        }
    )
}

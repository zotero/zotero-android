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
    libraryName: String,
    navigateToLibraries: () -> Unit,
    onAdd: () -> Unit,
) {
    NewCustomTopBar(
        title = libraryName,
        leftContainerContent = listOf {
            NewHeadingTextButton(
                onClick = navigateToLibraries,
                text = stringResource(id = Strings.toolbar_libraries)
            )
        },
        rightContainerContent = listOf {
            IconWithPadding(drawableRes = Drawables.add_24px, onClick = onAdd)
        }
    )
}

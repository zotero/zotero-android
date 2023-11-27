package org.zotero.android.screens.itemdetails

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.topbar.NewCustomTopBar
import org.zotero.android.uicomponents.topbar.NewHeadingTextButton

@Composable
internal fun ItemDetailsTopBar(
    onViewOrEditClicked: () -> Unit,
    onCancelOrBackClicked: () -> Unit,
    isEditing: Boolean,
) {
    NewCustomTopBar(
        leftContainerContent = listOf {
            ItemDetailsTopBarEditing(onCancelOrBackClicked, isEditing)
        },
        rightContainerContent = listOf {
            NewHeadingTextButton(
                isEnabled = true,
                onClick = onViewOrEditClicked,
                text = if (isEditing) {
                    stringResource(Strings.save)
                } else {
                    stringResource(Strings.edit)
                }
            )
        },
    )
}

@Composable
private fun ItemDetailsTopBarEditing(onCancelOrBackClicked: () -> Unit, isEditing: Boolean) {
    NewHeadingTextButton(
        isEnabled = true,
        onClick = onCancelOrBackClicked,
        text = if (isEditing) {
            stringResource(Strings.cancel)
        } else {
            val layoutType = CustomLayoutSize.calculateLayoutType()
            if (layoutType.isTablet()) {
                stringResource(Strings.back)
            } else {
                stringResource(Strings.collections_all_items)
            }
        }
    )
}
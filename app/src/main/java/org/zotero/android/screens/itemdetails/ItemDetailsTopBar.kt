package org.zotero.android.screens.itemdetails

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.screens.itemdetails.data.DetailType
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.topbar.NewCustomTopBar
import org.zotero.android.uicomponents.topbar.NewHeadingTextButton

@Composable
internal fun ItemDetailsTopBar(
    type: DetailType,
    onViewOrEditClicked: () -> Unit,
    onCancelOrBackClicked: () -> Unit,
    isEditing: Boolean,
) {
    NewCustomTopBar(
        leftContainerContent = listOf {
            ItemDetailsTopBarEditing(
                type = type,
                onCancelOrBackClicked = onCancelOrBackClicked,
                isEditing = isEditing
            )
        },
        rightContainerContent = listOf {
            NewHeadingTextButton(
                isEnabled = true,
                onClick = onViewOrEditClicked,
                text = if (isEditing) {
                    stringResource(Strings.done)
                } else {
                    stringResource(Strings.edit)
                }
            )
        },
    )
}

@Composable
private fun ItemDetailsTopBarEditing(
    type: DetailType,
    onCancelOrBackClicked: () -> Unit,
    isEditing: Boolean
) {
    if (isEditing) {
        when (type) {
            is DetailType.creation, is DetailType.duplication -> {
                NewHeadingTextButton(
                    onClick = onCancelOrBackClicked,
                    text = stringResource(Strings.cancel)
                )
            }

            else -> {
                //no-op
            }
        }
    } else {
        val layoutType = CustomLayoutSize.calculateLayoutType()
        val text = if (layoutType.isTablet()) {
            stringResource(Strings.back_button)
        } else {
            stringResource(Strings.collections_all_items)
        }
        NewHeadingTextButton(
            onClick = onCancelOrBackClicked,
            text = text
        )
    }
}
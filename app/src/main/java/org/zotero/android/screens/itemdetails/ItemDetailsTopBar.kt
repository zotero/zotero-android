package org.zotero.android.screens.itemdetails

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.topbar.HeadingTextButton

@Composable
internal fun ItemDetailsTopBar(
    onViewOrEditClicked: () -> Unit,
    onCancelOrBackClicked: () -> Unit,
    isEditing: Boolean,
    elevation: Dp = AppBarDefaults.TopAppBarElevation,
) {
    TopAppBar(
        title = {
            ItemDetailsTopBarEditing(onCancelOrBackClicked, isEditing)
        },
        actions = {
            HeadingTextButton(
                isEnabled = true,
                onClick = onViewOrEditClicked,
                text = if (isEditing) {
                    stringResource(Strings.save)
                } else {
                    stringResource(Strings.edit)
                }
            )
            Spacer(modifier = Modifier.width(if (isEditing) 4.dp else 0.dp))
        },
        backgroundColor = CustomTheme.colors.surface,
        elevation = elevation,
    )

}

@Composable
private fun ItemDetailsTopBarEditing(onCancelOrBackClicked: () -> Unit, isEditing: Boolean) {
    HeadingTextButton(
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
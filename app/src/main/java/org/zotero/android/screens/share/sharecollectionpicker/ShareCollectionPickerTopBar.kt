package org.zotero.android.screens.share.sharecollectionpicker

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.topbar.NewCustomTopBar
import org.zotero.android.uicomponents.topbar.NewHeadingTextButton

@Composable
internal fun ShareCollectionPickerTopBar(
    onBackClicked: () -> Unit,
) {
    NewCustomTopBar(
        leftContainerContent = listOf {
            NewHeadingTextButton(
                text = stringResource(id = Strings.back_button),
                onClick = onBackClicked
            )
        },
        leftGuidelineStartPercentage = 0.2f,
        rightGuidelineStartPercentage = 0.2f,
    )
}
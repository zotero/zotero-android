package org.zotero.android.screens.collectionpicker

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.topbar.NewCustomTopBar
import org.zotero.android.uicomponents.topbar.NewHeadingTextButton

@Composable
internal fun CollectionPickerTopBar(
    title: String,
    multipleSelectionAllowed: Boolean,
    onCancelClicked: () -> Unit,
    onAdd: () -> Unit,

) {
    NewCustomTopBar(
        title = title,
        leftContainerContent = listOf {
            NewHeadingTextButton(
                text = stringResource(id = Strings.cancel),
                onClick = onCancelClicked
            )
        },
        rightContainerContent = listOf {
            if (multipleSelectionAllowed) {
                NewHeadingTextButton(
                    text = stringResource(id = Strings.add),
                    onClick = onAdd
                )
            }
        },
        leftGuidelineStartPercentage = 0.2f,
        rightGuidelineStartPercentage = 0.2f,
    )
}
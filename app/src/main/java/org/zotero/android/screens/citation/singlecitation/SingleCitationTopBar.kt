package org.zotero.android.screens.citation.singlecitation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.topbar.NewCustomTopBar
import org.zotero.android.uicomponents.topbar.NewHeadingTextButton

@Composable
internal fun SingleCitationTopBar(
    onCancelClicked: () -> Unit,
    onCopyTapped: () -> Unit,

    ) {
    NewCustomTopBar(
        title = stringResource(Strings.citation_title),
        leftContainerContent = listOf {
            NewHeadingTextButton(
                text = stringResource(id = Strings.cancel),
                onClick = onCancelClicked
            )
        },
        rightContainerContent = listOf {
            NewHeadingTextButton(
                text = stringResource(id = Strings.copy_1),
                onClick = onCopyTapped
            )
        },
        leftGuidelineStartPercentage = 0.2f,
        rightGuidelineStartPercentage = 0.2f,
    )
}
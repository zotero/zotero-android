package org.zotero.android.pdf.reader.plainreader

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.topbar.NewCustomTopBar
import org.zotero.android.uicomponents.topbar.NewHeadingTextButton

@Composable
internal fun PdfPlainReaderTopBar(
    onBack: () -> Unit,
    title: String,
) {
    NewCustomTopBar(
        title = title,
        backgroundColor = CustomTheme.colors.surface,
        leftGuidelineStartPercentage = 0.2f,
        rightGuidelineStartPercentage = 0.05f,
        leftContainerContent = listOf(
            {
                NewHeadingTextButton(
                    onClick = onBack,
                    text = stringResource(Strings.back_button),
                )
            },
        )
    )
}

package org.zotero.android.pdffilter

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.topbar.HeadingTextButton

@Composable
internal fun PdfFilterScreenTopBar(
    onClose: () -> Unit,
    onClear: (() -> Unit)?,
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = stringResource(id = Strings.pdf_annotations_sidebar_filter_title),
                color = CustomTheme.colors.primaryContent,
                style = CustomTheme.typography.h2
            )
        },
        navigationIcon = {
            Row {
                Spacer(modifier = Modifier.width(8.dp))
                HeadingTextButton(
                    onClick = onClose,
                    text = stringResource(Strings.close),
                )
            }
        },
        actions = {
            if (onClear != null) {
                HeadingTextButton(
                    onClick = onClear,
                    text = stringResource(Strings.clear),
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = CustomTheme.colors.surface),
    )
}

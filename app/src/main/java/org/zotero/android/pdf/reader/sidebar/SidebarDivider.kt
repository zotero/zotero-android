package org.zotero.android.pdf.reader.sidebar

import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
fun SidebarDivider(modifier: Modifier = Modifier) {
    Divider(
        modifier = modifier,
        color = CustomTheme.colors.pdfAnnotationsDividerBackground,
        thickness = 1.dp
    )
}
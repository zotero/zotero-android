package org.zotero.android.pdf.annotation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.pdf.SidebarDivider
import org.zotero.android.pdf.data.Annotation
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun TagsAndCommentsSection(
    annotation: Annotation,
    layoutType: CustomLayoutSize.LayoutType
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 0.dp, bottom = 8.dp)
    ) {
        Text(
            modifier = Modifier.padding(start = 8.dp),
            text = if (annotation.comment.isBlank()) {
                stringResource(id = Strings.no_comments)
            } else {
                annotation.comment
            },
            color = CustomTheme.colors.primaryContent,
            style = CustomTheme.typography.default,
            fontSize = layoutType.calculatePdfSidebarTextSize(),
        )
        Spacer(modifier = Modifier.height(4.dp))
        SidebarDivider(
            modifier = Modifier
                .height(1.dp)
                .fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(4.dp))
        TagsSection(annotation, layoutType)

    }
}

@Composable
internal fun TagsSection(
    annotation: Annotation,
    layoutType: CustomLayoutSize.LayoutType
) {
    Text(
        modifier = Modifier.padding(start = 8.dp),
        text = if (annotation.tags.isEmpty()) {
            stringResource(id = Strings.no_tags)
        } else {
            annotation.tags.joinToString(
                separator = ", "
            ) { it.name }
        },
        color = CustomTheme.colors.primaryContent,
        style = CustomTheme.typography.default,
        fontSize = layoutType.calculatePdfSidebarTextSize(),
    )
}


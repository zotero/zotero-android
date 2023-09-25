package org.zotero.android.pdf.annotation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.zotero.android.R
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.database.objects.AnnotationType
import org.zotero.android.pdf.data.Annotation
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.topbar.HeadingTextButton

@Composable
internal fun PdfAnnotationHeaderRow(
    annotation: Annotation,
    annotationColor: Color,
    layoutType: CustomLayoutSize.LayoutType,
    onBack: () -> Unit,
) {
    val title = stringResource(R.string.page) + " " + annotation.pageLabel
    val icon = when (annotation.type) {
        AnnotationType.note -> Drawables.note_large
        AnnotationType.highlight -> Drawables.highlighter_large
        AnnotationType.image -> Drawables.area_large
        AnnotationType.ink -> Drawables.ink_large
    }
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(8.dp))
            Image(
                modifier = Modifier.size(layoutType.calculatePdfSidebarHeaderIconSize()),
                painter = painterResource(id = icon),
                contentDescription = null,
                colorFilter = ColorFilter.tint(annotationColor),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                color = CustomTheme.colors.primaryContent,
                style = CustomTheme.typography.defaultBold,
                fontSize = layoutType.calculatePdfSidebarTextSize(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(modifier = Modifier.align(Alignment.CenterEnd)) {
            HeadingTextButton(
                onClick = onBack,
                text = stringResource(Strings.done)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

    }
}

@Composable
internal fun PdfAnnotationNoteRow(annotation: Annotation, layoutType: CustomLayoutSize.LayoutType) {
    TagsAndCommentsSection(annotation = annotation, layoutType = layoutType)
}

@Composable
internal fun PdfAnnotationHighlightRow(
    annotation: Annotation,
    layoutType: CustomLayoutSize.LayoutType,
) {
    TagsAndCommentsSection(annotation = annotation, layoutType = layoutType)
}

@Composable
internal fun PdfAnnotationInkRow(
    annotation: Annotation,
    layoutType: CustomLayoutSize.LayoutType,
) {
    TagsSection(annotation = annotation, layoutType = layoutType)
}

@Composable
internal fun PdfAnnotationImageRow(
    annotation: Annotation,
    layoutType: CustomLayoutSize.LayoutType,
) {
    TagsAndCommentsSection(annotation = annotation, layoutType = layoutType)
}



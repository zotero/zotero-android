package org.zotero.android.pdf.reader.sidebar.sections

import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.zotero.android.database.objects.AnnotationType
import org.zotero.android.pdf.data.PDFAnnotation
import org.zotero.android.pdf.reader.PdfReaderVMInterface
import org.zotero.android.pdf.reader.PdfReaderViewState
import org.zotero.android.pdf.reader.sidebar.sectionHorizontalPadding
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun PdfReaderAnnotationsSidebarHeaderSection(
    vMInterface: PdfReaderVMInterface,
    viewState: PdfReaderViewState,
    annotation: PDFAnnotation,
    annotationColor: Color,
) {
    val title = stringResource(Strings.page) + " " + annotation.pageLabel
    val icon = when (annotation.type) {
        AnnotationType.note -> Drawables.annotate_note
        AnnotationType.highlight -> Drawables.annotate_highlight
        AnnotationType.image -> Drawables.annotate_area
        AnnotationType.ink -> Drawables.annotate_ink
        AnnotationType.underline -> Drawables.annotate_underline
        AnnotationType.text -> Drawables.annotate_text
    }
    Row(
        modifier = Modifier
            .height(36.dp)
            .sectionHorizontalPadding(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            modifier = Modifier.size(24.dp),
            painter = painterResource(id = icon),
            contentDescription = null,
            colorFilter = ColorFilter.tint(annotationColor),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.weight(1f))
        if (viewState.isAnnotationSelected(annotation.key)) {
            if (annotation.isZoteroAnnotation) {
                Image(
                    modifier = Modifier
                        .size(22.dp)
                        .safeClickable(
                            onClick = vMInterface::onMoreOptionsForItemClicked,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = false)
                        ),
                    painter = painterResource(id = Drawables.more_horiz_24px),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
                )
            }
        }
        if (!annotation.isZoteroAnnotation) {
            Image(
                modifier = Modifier
                    .size(22.dp),
                painter = painterResource(id = Drawables.ic_lock_solid),
                contentDescription = null,
                colorFilter = ColorFilter.tint(CustomTheme.colors.disabledContent),
            )
        }
    }
}
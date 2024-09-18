package org.zotero.android.pdf.reader.sidebar

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.zotero.android.R
import org.zotero.android.androidx.content.pxToDp
import org.zotero.android.database.objects.AnnotationType
import org.zotero.android.pdf.data.PDFAnnotation
import org.zotero.android.pdf.reader.PdfReaderVMInterface
import org.zotero.android.pdf.reader.PdfReaderViewState
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.textinput.CustomTextField
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun PdfReaderAnnotationsSidebarHeaderSection(
    vMInterface: PdfReaderVMInterface,
    viewState: PdfReaderViewState,
    annotation: PDFAnnotation,
    annotationColor: Color,
) {
    val title = stringResource(R.string.page) + " " + annotation.pageLabel
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
            modifier = Modifier.size(22.dp),
            painter = painterResource(id = icon),
            contentDescription = null,
            colorFilter = ColorFilter.tint(annotationColor),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = CustomTheme.colors.primaryContent,
            style = CustomTheme.typography.newH4,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (viewState.isAnnotationSelected(annotation.key)) {
            Spacer(modifier = Modifier.weight(1f))
            Image(
                modifier = Modifier
                    .size(22.dp)
                    .safeClickable(
                        onClick = vMInterface::onMoreOptionsForItemClicked,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple(bounded = false)
                    ),
                painter = painterResource(id = Drawables.more_horiz_24px),
                contentDescription = null,
                colorFilter = ColorFilter.tint(CustomTheme.colors.zoteroDefaultBlue),
            )
        }
    }
}

@Composable
internal fun PdfReaderAnnotationsSidebarTagsAndCommentsSection(
    vMInterface: PdfReaderVMInterface,
    viewState: PdfReaderViewState,
    annotation: PDFAnnotation,
    shouldAddTopPadding: Boolean,
) {
    PdfReaderAnnotationsSidebarCommentSection(
        annotation = annotation,
        shouldAddTopPadding = shouldAddTopPadding,
        vMInterface = vMInterface,
        viewState = viewState,
    )
    PdfReaderAnnotationsSidebarTagsSection(
        annotation = annotation,
        vMInterface = vMInterface,
        viewState = viewState,
    )
}

@Composable
private fun PdfReaderAnnotationsSidebarCommentSection(
    vMInterface: PdfReaderVMInterface,
    viewState: PdfReaderViewState,
    annotation: PDFAnnotation,
    shouldAddTopPadding: Boolean,
) {
    if (viewState.isAnnotationSelected(annotation.key)) {
        CustomTextField(
            modifier = Modifier
                .sectionHorizontalPadding()
                .padding(bottom = 8.dp)
                .padding(top = if (shouldAddTopPadding) 8.dp else 0.dp)
                .onFocusChanged {
                    if (it.hasFocus) {
                        vMInterface.onCommentFocusFieldChange(annotation.key)
                    }
                },
            value = if (annotation.key == viewState.commentFocusKey) {
                viewState.commentFocusText
            } else {
                annotation.comment
            },
            textStyle = CustomTheme.typography.newInfo,
            hint = stringResource(id = Strings.pdf_annotations_sidebar_add_comment),
            hintColor = CustomTheme.colors.zoteroDefaultBlue,
            ignoreTabsAndCaretReturns = false,
            onValueChange = { vMInterface.onCommentTextChange(annotationKey = annotation.key, it) })
    } else if (annotation.comment.isNotBlank()) {
        Text(
            modifier = Modifier
                .sectionHorizontalPadding()
                .padding(bottom = 8.dp)
                .padding(top = if (shouldAddTopPadding) 8.dp else 0.dp),
            text = annotation.comment,
            color = CustomTheme.colors.primaryContent,
            style = CustomTheme.typography.newInfo,
        )
    }
}

@Composable
internal fun PdfReaderAnnotationsSidebarTagsSection(
    annotation: PDFAnnotation,
    vMInterface: PdfReaderVMInterface,
    viewState: PdfReaderViewState,
) {
    val isSelected = viewState.isAnnotationSelected(annotation.key)
    val areTagsPresent = annotation.tags.isNotEmpty()
    val shouldDisplayTagsSection = isSelected || areTagsPresent
    if (shouldDisplayTagsSection) {
        SidebarDivider()
    }
    if (shouldDisplayTagsSection) {
        Box(modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true),
                onClick = { vMInterface.onTagsClicked(annotation) }
            )
            .sectionVerticalPadding()
            .fillMaxWidth()
            .height(22.dp)
        ) {
            if (areTagsPresent) {
                Text(
                    modifier = Modifier
                        .sectionHorizontalPadding(),
                    text = annotation.tags.joinToString(separator = ", ") { it.name },
                    color = CustomTheme.colors.zoteroDefaultBlue,
                    style = CustomTheme.typography.newInfo,
                )
            } else {
                Text(
                    modifier = Modifier
                        .sectionHorizontalPadding(),
                    text = stringResource(id = Strings.pdf_annotations_sidebar_add_tags),
                    color = CustomTheme.colors.zoteroDefaultBlue,
                    style = CustomTheme.typography.newInfo,
                )
            }
        }
    }
}

@Composable
internal fun PdfReaderAnnotationsSidebarHighlightedTextSection(
    annotationColor: Color,
    annotation: PDFAnnotation,
) {
    Box(
        modifier = Modifier
            .sectionHorizontalPadding()
            .sectionVerticalPadding()
            .height(IntrinsicSize.Max)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(annotationColor)
        )
        Text(
            modifier = Modifier.padding(start = 8.dp),
            text = annotation.text ?: "",
            color = Color(0xFF6E6D73),
            style = CustomTheme.typography.newInfo,
        )
    }
}

@Composable
internal fun PdfReaderAnnotationsSidebarImageSection(
    loadPreview: () -> Bitmap?,
    vMInterface: PdfReaderVMInterface,
) {
    val cachedBitmap = loadPreview()
    if (cachedBitmap != null) {
        Image(
            modifier = Modifier
                .sectionHorizontalPadding()
                .sectionVerticalPadding()
                .fillMaxWidth()
                .heightIn(max = vMInterface.annotationMaxSideSize.pxToDp()),
            bitmap = cachedBitmap.asImageBitmap(),
            contentDescription = null,
        )
    }
}

@Composable
internal fun PdfReaderAnnotationsSidebarUnderlineTextSection(
    annotationColor: Color,
    annotation: PDFAnnotation,
) {
    Box(
        modifier = Modifier
            .sectionHorizontalPadding()
            .sectionVerticalPadding()
            .height(IntrinsicSize.Max)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(annotationColor)
        )
        Text(
            modifier = Modifier.padding(start = 8.dp),
            text = annotation.text ?: "",
            color = Color(0xFF6E6D73),
            style = CustomTheme.typography.newInfo,
        )
    }
}


internal fun Modifier.sectionHorizontalPadding(): Modifier {
    return this.padding(horizontal = 8.dp)
}

internal fun Modifier.sectionVerticalPadding(): Modifier {
    return this.padding(vertical = 8.dp)
}
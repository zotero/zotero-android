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
import androidx.compose.ui.focus.FocusRequester
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
import org.zotero.android.pdf.data.Annotation
import org.zotero.android.pdf.reader.PdfReaderViewModel
import org.zotero.android.pdf.reader.PdfReaderViewState
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.textinput.CustomTextField
import org.zotero.android.uicomponents.theme.CustomTheme

@Composable
internal fun SidebarHeaderSection(
    viewModel: PdfReaderViewModel,
    viewState: PdfReaderViewState,
    annotation: Annotation,
    annotationColor: Color,
) {
    val title = stringResource(R.string.page) + " " + annotation.pageLabel
    val icon = when (annotation.type) {
        AnnotationType.note -> Drawables.note_large
        AnnotationType.highlight -> Drawables.highlighter_large
        AnnotationType.image -> Drawables.area_large
        AnnotationType.ink -> Drawables.ink_large
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
                        onClick = { viewModel.onMoreOptionsForItemClicked() },
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
internal fun SidebarTagsAndCommentsSection(
    annotation: Annotation,
    viewModel: PdfReaderViewModel,
    viewState: PdfReaderViewState,
    focusRequester: FocusRequester,
    shouldAddTopPadding: Boolean,
) {
    SidebarCommentSection(
        viewModel = viewModel,
        annotation = annotation,
        viewState = viewState,
        focusRequester = focusRequester,
        shouldAddTopPadding = shouldAddTopPadding
    )
    SidebarTagsSection(viewModel = viewModel, viewState = viewState, annotation = annotation)
}

@Composable
private fun SidebarCommentSection(
    viewModel: PdfReaderViewModel,
    viewState: PdfReaderViewState,
    annotation: Annotation,
    shouldAddTopPadding: Boolean,
    focusRequester: FocusRequester
) {
    if (viewState.isAnnotationSelected(annotation.key)) {
        CustomTextField(
            modifier = Modifier
                .sectionHorizontalPadding()
                .padding(bottom = 8.dp)
                .padding(top = if (shouldAddTopPadding) 8.dp else 0.dp)
                .onFocusChanged {
                    if (it.hasFocus) {
                        viewModel.onCommentFocusFieldChange(annotation.key)
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
            focusRequester = focusRequester,
            ignoreTabsAndCaretReturns = false,
            onValueChange = { viewModel.onCommentTextChange(annotationKey = annotation.key, it) })
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
internal fun SidebarTagsSection(
    viewModel: PdfReaderViewModel,
    viewState: PdfReaderViewState,
    annotation: Annotation,
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
                onClick = { viewModel.onTagsClicked(annotation) }
            )
            .sectionVerticalPadding()
            .fillMaxWidth()
            .height(22.dp)
           ,
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
internal fun SidebarHighlightedTextSection(
    annotationColor: Color,
    annotation: Annotation,
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
internal fun SidebarImageSection(
    loadPreview: () -> Bitmap?,
    viewModel: PdfReaderViewModel
) {
    val cachedBitmap = loadPreview()
    if (cachedBitmap != null) {
        Image(
            modifier = Modifier
                .sectionHorizontalPadding()
                .sectionVerticalPadding()
                .fillMaxWidth()
                .heightIn(max = viewModel.annotationMaxSideSize.pxToDp()),
            bitmap = cachedBitmap.asImageBitmap(),
            contentDescription = null,
        )
    }
}


private fun Modifier.sectionHorizontalPadding(): Modifier {
    return this.padding(horizontal = 8.dp)
}
private fun Modifier.sectionVerticalPadding(): Modifier {
    return this.padding(vertical = 8.dp)
}
package org.zotero.android.pdf.annotation

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.database.objects.AnnotationType
import org.zotero.android.pdf.annotation.data.PdfAnnotationArgs
import org.zotero.android.pdf.annotation.row.PdfAnnotationHeaderRow
import org.zotero.android.pdf.annotation.row.PdfAnnotationHighlightRow
import org.zotero.android.pdf.annotation.row.PdfAnnotationImageRow
import org.zotero.android.pdf.annotation.row.PdfAnnotationInkRow
import org.zotero.android.pdf.annotation.row.PdfAnnotationNoteRow
import org.zotero.android.pdf.annotation.row.PdfAnnotationTextRow
import org.zotero.android.pdf.annotation.row.PdfAnnotationUnderlineRow
import org.zotero.android.pdf.data.PDFAnnotation
import org.zotero.android.screens.settings.elements.NewSettingsDivider
import org.zotero.android.sync.Tag
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.themem3.AppThemeM3

@Composable
internal fun PdfAnnotationScreen(
    viewModel: PdfAnnotationViewModel = hiltViewModel(),
    args: PdfAnnotationArgs,
    navigateToTagPicker: () -> Unit,
    onBack: () -> Unit,
) {
    val layoutType = CustomLayoutSize.calculateLayoutType()
    val isTablet = layoutType.isTablet()
    LaunchedEffect(args) {
        viewModel.init(args = args, isTablet = isTablet)
    }

    viewModel.setOsTheme(isDark = isSystemInDarkTheme())
    val viewState by viewModel.viewStates.observeAsState(PdfAnnotationViewState())
    val viewEffect by viewModel.viewEffects.observeAsState()
    AppThemeM3 {
        LaunchedEffect(key1 = viewEffect) {
            when (viewEffect?.consume()) {
                is PdfAnnotationViewEffect.NavigateToTagPickerScreen -> {
                    navigateToTagPicker()
                }
                is PdfAnnotationViewEffect.Back -> {
                    onBack()
                }
                else -> {}
            }
        }
        val stateAnnotation = viewState.annotation
        val selectedColor = viewState.color
        PdfAnnotationPart(
            stateAnnotation = stateAnnotation,
            onDone = viewModel::onDone,
            fontSize = viewState.fontSize,
            onFontSizeDecrease = viewModel::onFontSizeDecrease,
            onFontSizeIncrease = viewModel::onFontSizeIncrease,
            onColorSelected = viewModel::onColorSelected,
            colors = viewState.colors,
            selectedColor = selectedColor,
            tags = viewState.tags,
            onTagsClicked = viewModel::onTagsClicked,
            commentFocusText = viewState.commentFocusText,
            onCommentTextChange = viewModel::onCommentTextChange,
            onSizeChanged = viewModel::onSizeChanged,
            onDeleteAnnotation = viewModel::onDeleteAnnotation,
            size = viewState.size
        )
    }
}

@Composable
internal fun PdfAnnotationPart(
    stateAnnotation: PDFAnnotation?,
    onDone: () -> Unit,
    onDeleteAnnotation: () -> Unit,
    fontSize: Float,
    onFontSizeDecrease: () -> Unit,
    onFontSizeIncrease: () -> Unit,
    onColorSelected: (String) -> Unit,
    colors: List<String>,
    selectedColor: String,
    tags: List<Tag>,
    onTagsClicked: () -> Unit,
    commentFocusText: String,
    onCommentTextChange: (String) -> Unit,
    size: Float,
    onSizeChanged: (Float) -> Unit,
) {
    val annotation = stateAnnotation ?: return

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(TopAppBarDefaults.windowInsets)
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        val annotationColor =
            Color(annotation.displayColor.toColorInt())
        item {
            PdfAnnotationHeaderRow(
                annotation = annotation,
                annotationColor = annotationColor,
                onBack = onDone,
            )
        }

        item {
            when (annotation.type) {
                AnnotationType.note -> PdfAnnotationNoteRow(
                    annotation = annotation,
                    onColorSelected = onColorSelected,
                    onCommentTextChange = onCommentTextChange,
                    selectedColor = selectedColor,
                    colors = colors,
                    commentFocusText = commentFocusText,
                    tags = tags,
                    onTagsClicked = onTagsClicked
                )

                AnnotationType.highlight -> PdfAnnotationHighlightRow(
                    annotation = annotation,
                    onColorSelected = onColorSelected,
                    onCommentTextChange = onCommentTextChange,
                    selectedColor = selectedColor,
                    colors = colors,
                    commentFocusText = commentFocusText,
                    tags = tags,
                    onTagsClicked = onTagsClicked
                )

                AnnotationType.ink -> PdfAnnotationInkRow(
                    annotation = annotation,
                    onCommentTextChange = onCommentTextChange,
                    commentFocusText = commentFocusText,
                    tags = tags,
                    onTagsClicked = onTagsClicked,
                    size = size,
                    onSizeChanged = onSizeChanged
                )

                AnnotationType.image -> PdfAnnotationImageRow(
                    annotation = annotation,
                    onColorSelected = onColorSelected,
                    onCommentTextChange = onCommentTextChange,
                    selectedColor = selectedColor,
                    colors = colors,
                    commentFocusText = commentFocusText,
                    tags = tags,
                    onTagsClicked = onTagsClicked
                )
                AnnotationType.underline -> PdfAnnotationUnderlineRow(
                    annotation = annotation,
                    onColorSelected = onColorSelected,
                    onCommentTextChange = onCommentTextChange,
                    selectedColor = selectedColor,
                    colors = colors,
                    commentFocusText = commentFocusText,
                    tags = tags,
                    onTagsClicked = onTagsClicked
                )
                AnnotationType.text -> PdfAnnotationTextRow(
                    annotation = annotation,
                    fontSize = fontSize,
                    onFontSizeDecrease = onFontSizeDecrease,
                    onFontSizeIncrease = onFontSizeIncrease,
                    onColorSelected = onColorSelected,
                    colors = colors,
                    selectedColor = selectedColor,
                    tags = tags,
                    onTagsClicked = onTagsClicked
                )
            }
        }
        item {
            if (annotation.isZoteroAnnotation) {
                NewSettingsDivider()
                DeleteButton(onDeleteAnnotation = onDeleteAnnotation)
            }

        }

    }
}

@Composable
private fun DeleteButton(onDeleteAnnotation: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .safeClickable(
                onClick = onDeleteAnnotation,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true)
            ), contentAlignment = Alignment.CenterStart
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = stringResource(Strings.pdf_annotation_popover_delete),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
package org.zotero.android.pdf.annotation

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
import org.zotero.android.pdf.reader.sidebar.SidebarDivider
import org.zotero.android.sync.Tag
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.theme.CustomPalette
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.theme.CustomThemeWithStatusAndNavBars
import org.zotero.android.uicomponents.topbar.HeadingTextButton

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
    CustomThemeWithStatusAndNavBars(isDarkTheme = viewState.isDark) {
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
    val layoutType = CustomLayoutSize.calculateLayoutType()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CustomTheme.colors.surface)
    ) {
        val annotationColor =
            Color(android.graphics.Color.parseColor(annotation.displayColor))
        item {
            PdfAnnotationHeaderRow(
                annotation = annotation,
                annotationColor = annotationColor,
                layoutType = layoutType,
                onBack = onDone,
            )
            if (annotation.type != AnnotationType.text) {
                SidebarDivider(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(4.dp))
            }
        }


        item {
            when (annotation.type) {
                AnnotationType.note -> PdfAnnotationNoteRow(
                    annotation = annotation,
                    layoutType = layoutType,
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
                    layoutType = layoutType,
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
                    layoutType = layoutType,
                    onCommentTextChange = onCommentTextChange,
                    commentFocusText = commentFocusText,
                    tags = tags,
                    onTagsClicked = onTagsClicked,
                    size = size,
                    onSizeChanged = onSizeChanged
                )

                AnnotationType.image -> PdfAnnotationImageRow(
                    annotation = annotation,
                    layoutType = layoutType,
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
                    layoutType = layoutType,
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
                    layoutType = layoutType,
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
            if (annotation.type != AnnotationType.text) {
                SidebarDivider(
                    modifier = Modifier
                        .height(1.dp)
                        .fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            if (annotation.isZoteroAnnotation) {
                HeadingTextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDeleteAnnotation,
                    contentColor = CustomPalette.ErrorRed,
                    text = stringResource(Strings.pdf_annotation_popover_delete),
                )
            }

        }

    }
}
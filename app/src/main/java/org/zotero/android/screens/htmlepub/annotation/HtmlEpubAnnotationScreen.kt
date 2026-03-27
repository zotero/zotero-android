package org.zotero.android.screens.htmlepub.annotation

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
import org.zotero.android.screens.htmlepub.annotation.data.HtmlEpubAnnotationArgs
import org.zotero.android.screens.htmlepub.reader.data.HtmlEpubAnnotation
import org.zotero.android.screens.settings.elements.NewSettingsDivider
import org.zotero.android.sync.Tag
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.themem3.AppThemeM3

@Composable
internal fun HtmlEpubAnnotationScreen(
    viewModel: HtmlEpubAnnotationViewModel = hiltViewModel(),
    args: HtmlEpubAnnotationArgs,
    navigateToTagPicker: () -> Unit,
    onBack: () -> Unit,
) {
    val layoutType = CustomLayoutSize.calculateLayoutType()
    val isTablet = layoutType.isTablet()
    LaunchedEffect(args) {
        viewModel.init(args = args, isTablet = isTablet)
    }

    viewModel.setOsTheme(isDark = isSystemInDarkTheme())
    val viewState by viewModel.viewStates.observeAsState(HtmlEpubAnnotationViewState())
    val viewEffect by viewModel.viewEffects.observeAsState()
    AppThemeM3 {
        LaunchedEffect(key1 = viewEffect) {
            when (viewEffect?.consume()) {
                is HtmlEpubAnnotationViewEffect.NavigateToTagPickerScreen -> {
                    navigateToTagPicker()
                }

                is HtmlEpubAnnotationViewEffect.Back -> {
                    onBack()
                }

                else -> {}
            }
        }
        val stateAnnotation = viewState.annotation
        val selectedColor = viewState.color
        HtmlEpubAnnotationPart(
            stateAnnotation = stateAnnotation,
            onDone = viewModel::onDone,
            onColorSelected = viewModel::onColorSelected,
            colors = viewState.colors,
            selectedColor = selectedColor,
            tags = viewState.tags,
            onTagsClicked = viewModel::onTagsClicked,
            commentFocusText = viewState.commentFocusText,
            onCommentTextChange = viewModel::onCommentTextChange,
            onDeleteAnnotation = viewModel::onDeleteAnnotation,
        )
    }
}

@Composable
internal fun HtmlEpubAnnotationPart(
    stateAnnotation: HtmlEpubAnnotation?,
    onDone: () -> Unit,
    onDeleteAnnotation: () -> Unit,
    onColorSelected: (String) -> Unit,
    colors: List<String>,
    selectedColor: String,
    tags: List<Tag>,
    onTagsClicked: () -> Unit,
    commentFocusText: String,
    onCommentTextChange: (String) -> Unit,
) {
    val annotation = stateAnnotation ?: return

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(TopAppBarDefaults.windowInsets)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        val annotationColor =
            Color(annotation.displayColor.toColorInt())
        item {
            HtmlEpubAnnotationHeaderRow(
                annotation = annotation,
                annotationColor = annotationColor,
                onBack = onDone,
            )
        }

        item {
            when (annotation.type) {
                AnnotationType.note -> HtmlEpubAnnotationNoteRow(
                    annotation = annotation,
                    onColorSelected = onColorSelected,
                    onCommentTextChange = onCommentTextChange,
                    selectedColor = selectedColor,
                    colors = colors,
                    commentFocusText = commentFocusText,
                    tags = tags,
                    onTagsClicked = onTagsClicked
                )

                AnnotationType.highlight -> HtmlEpubAnnotationHighlightRow(
                    annotation = annotation,
                    onColorSelected = onColorSelected,
                    onCommentTextChange = onCommentTextChange,
                    selectedColor = selectedColor,
                    colors = colors,
                    commentFocusText = commentFocusText,
                    tags = tags,
                    onTagsClicked = onTagsClicked
                )

                AnnotationType.underline -> HtmlEpubAnnotationUnderlineRow(
                    annotation = annotation,
                    onColorSelected = onColorSelected,
                    onCommentTextChange = onCommentTextChange,
                    selectedColor = selectedColor,
                    colors = colors,
                    commentFocusText = commentFocusText,
                    tags = tags,
                    onTagsClicked = onTagsClicked
                )

                else -> {}
            }
        }
        item {
            NewSettingsDivider()
            DeleteButton(onDeleteAnnotation = onDeleteAnnotation)

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
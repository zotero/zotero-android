package org.zotero.android.pdf.annotation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.R
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.database.objects.AnnotationType
import org.zotero.android.pdf.SidebarDivider
import org.zotero.android.pdf.data.Annotation
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.theme.CustomThemeWithStatusAndNavBars
import org.zotero.android.uicomponents.topbar.HeadingTextButton

@Composable
internal fun PdfAnnotationScreen(
    viewModel: PdfAnnotationViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    viewModel.init()
    viewModel.setOsTheme(isDark = isSystemInDarkTheme())
    val viewState by viewModel.viewStates.observeAsState(PdfAnnotationViewState())
    val viewEffect by viewModel.viewEffects.observeAsState()
    CustomThemeWithStatusAndNavBars(isDarkTheme = viewState.isDark) {
        LaunchedEffect(key1 = viewEffect) {
            when (viewEffect?.consume()) {
                PdfAnnotationViewEffect.NavigateBack -> onBack()
                null -> Unit
            }
        }
        PdfAnnotationPart(
            viewState = viewState,
            onBack = onBack
        )
    }

}

@Composable
internal fun PdfAnnotationPart(
    viewState: PdfAnnotationViewState,
    onBack: () -> Unit,
) {
    val annotation = viewState.annotation ?: return
    val layoutType = CustomLayoutSize.calculateLayoutType()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CustomTheme.colors.surface)
        ) {
            val annotationColor =
                Color(android.graphics.Color.parseColor(annotation.displayColor))
            PdfAnnotationHeaderRow(
                annotation = annotation,
                annotationColor = annotationColor,
                layoutType = layoutType,
                onBack = onBack,
            )
//            if (!annotation.tags.isEmpty() || !annotation.comment.isEmpty()) {
                SidebarDivider(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(4.dp))
//            }

            when (annotation.type) {
                AnnotationType.note -> PdfAnnotationNoteRow(
                    annotation = annotation,
                    layoutType = layoutType
                )

                AnnotationType.highlight -> PdfAnnotationHighlightRow(
                    annotation = annotation,
                    layoutType = layoutType,
                )

                AnnotationType.ink -> PdfAnnotationInkRow(
                    annotation = annotation,
                    layoutType = layoutType,
                )

                AnnotationType.image -> PdfAnnotationImageRow(
                    annotation = annotation,
                    layoutType = layoutType,
                )
            }
    }
}

@Composable
private fun PdfAnnotationNoteRow(annotation: Annotation, layoutType: CustomLayoutSize.LayoutType) {
    TagsAndCommentsSection(annotation, layoutType)
}

@Composable
fun PdfAnnotationHighlightRow(
    annotation: Annotation,
    layoutType: CustomLayoutSize.LayoutType,
) {
    TagsAndCommentsSection(annotation, layoutType)
}

@Composable
private fun PdfAnnotationInkRow(
    annotation: Annotation,
    layoutType: CustomLayoutSize.LayoutType,
) {
    TagsSection(annotation, layoutType)
}

@Composable
private fun PdfAnnotationImageRow(
    annotation: Annotation,
    layoutType: CustomLayoutSize.LayoutType,
) {
    TagsAndCommentsSection(annotation, layoutType)
}


@Composable
private fun PdfAnnotationHeaderRow(
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
private fun TagsAndCommentsSection(
    annotation: Annotation,
    layoutType: CustomLayoutSize.LayoutType
) {
//    val showTagsAndCommentsLayout = !annotation.tags.isEmpty() || !annotation.comment.isEmpty()
//    if (!showTagsAndCommentsLayout) {
//        return
//    }
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
//        if (!annotation.tags.isEmpty() && !annotation.comment.isEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            SidebarDivider(
                modifier = Modifier
                    .height(1.dp)
                    .fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
//        }
        TagsSection(annotation, layoutType)

    }
}

@Composable
private fun TagsSection(
    annotation: Annotation,
    layoutType: CustomLayoutSize.LayoutType
) {
//    if (!annotation.tags.isEmpty()) {
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
//    }
}



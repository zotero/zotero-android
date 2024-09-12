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
import org.zotero.android.pdf.reader.sidebar.SidebarDivider
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.theme.CustomPalette
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.theme.CustomThemeWithStatusAndNavBars
import org.zotero.android.uicomponents.topbar.HeadingTextButton

@Composable
internal fun PdfAnnotationScreen(
    viewModel: PdfAnnotationViewModel = hiltViewModel(),
    navigateToTagPicker: () -> Unit,
    onBack: () -> Unit,
) {
    viewModel.init()
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
        PdfAnnotationPart(
            viewState = viewState,
            viewModel = viewModel,
            onBack = onBack
        )
    }
}

@Composable
internal fun PdfAnnotationPart(
    viewState: PdfAnnotationViewState,
    viewModel: PdfAnnotationViewModel,
    onBack: () -> Unit,
) {
    val annotation = viewState.annotation ?: return
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
                onBack = onBack,
            )
            SidebarDivider(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(4.dp))
        }


        item {
            when (annotation.type) {
                AnnotationType.note -> PdfAnnotationNoteRow(
                    layoutType = layoutType,
                    viewModel = viewModel,
                    viewState = viewState,
                )

                AnnotationType.highlight -> PdfAnnotationHighlightRow(
                    layoutType = layoutType,
                    viewState = viewState,
                    viewModel = viewModel,
                )

                AnnotationType.ink -> PdfAnnotationInkRow(
                    viewModel = viewModel,
                    viewState = viewState,
                    layoutType = layoutType,
                )

                AnnotationType.image -> PdfAnnotationImageRow(
                    viewState = viewState,
                    viewModel = viewModel,
                    layoutType = layoutType,
                ) else -> {
                    //TODO freeText and underline Rows
                }
            }
        }
        item {
            SidebarDivider(
                modifier = Modifier
                    .height(1.dp)
                    .fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            HeadingTextButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = viewModel::onDeleteAnnotation,
                contentColor = CustomPalette.ErrorRed,
                text = stringResource(Strings.pdf_annotation_popover_delete),
            )
        }



    }
}
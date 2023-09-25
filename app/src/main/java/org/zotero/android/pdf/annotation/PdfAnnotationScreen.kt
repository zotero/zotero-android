package org.zotero.android.pdf.annotation

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.database.objects.AnnotationType
import org.zotero.android.pdf.SidebarDivider
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.theme.CustomThemeWithStatusAndNavBars

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
                null -> Unit
                else -> {}
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
        SidebarDivider(modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(4.dp))

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
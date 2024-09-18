package org.zotero.android.pdf.annotationmore

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.database.objects.AnnotationType
import org.zotero.android.pdf.annotationmore.rows.PdfAnnotationMoreFreeTextRow
import org.zotero.android.pdf.annotationmore.rows.PdfAnnotationMoreHighlightRow
import org.zotero.android.pdf.annotationmore.rows.PdfAnnotationMoreImageRow
import org.zotero.android.pdf.annotationmore.rows.PdfAnnotationMoreInkRow
import org.zotero.android.pdf.annotationmore.rows.PdfAnnotationMoreNoteRow
import org.zotero.android.pdf.annotationmore.rows.PdfAnnotationMoreUnderlineRow
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.theme.CustomPalette
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.theme.CustomThemeWithStatusAndNavBars

@Composable
internal fun PdfAnnotationMoreScreen(
    viewModel: PdfAnnotationMoreViewModel = hiltViewModel(),
    navigateToPageEdit: () -> Unit,
    onBack: () -> Unit,
) {
    viewModel.init()
    viewModel.setOsTheme(isDark = isSystemInDarkTheme())
    val viewState by viewModel.viewStates.observeAsState(PdfAnnotationMoreViewState())
    val viewEffect by viewModel.viewEffects.observeAsState()
    CustomThemeWithStatusAndNavBars(
        isDarkTheme = viewState.isDark,
        statusBarBackgroundColor = CustomTheme.colors.zoteroEditFieldBackground,
        navBarBackgroundColor = CustomTheme.colors.pdfAnnotationsFormBackground,
    ) {
        LaunchedEffect(key1 = viewEffect) {
            when (viewEffect?.consume()) {
                is PdfAnnotationMoreViewEffect.NavigateToPageEditScreen -> {
                    navigateToPageEdit()
                }

                is PdfAnnotationMoreViewEffect.Back -> {
                    onBack()
                }

                else -> {}
            }
        }

        CustomScaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                PdfAnnotationMoreTopBar(onBack = onBack, viewModel = viewModel)
            },
        ) {
            PdfAnnotationMorePart(
                viewState = viewState,
                viewModel = viewModel,
            )
        }
    }
}

@Composable
internal fun PdfAnnotationMorePart(
    viewState: PdfAnnotationMoreViewState,
    viewModel: PdfAnnotationMoreViewModel,
) {
    val layoutType = CustomLayoutSize.calculateLayoutType()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CustomTheme.colors.pdfAnnotationsFormBackground)
    ) {
        item {
            when (viewState.type) {
                AnnotationType.note -> {
                    PdfAnnotationMoreNoteRow(
                        viewModel = viewModel,
                        viewState = viewState,
                    )
                }

                AnnotationType.highlight -> {
                    PdfAnnotationMoreHighlightRow(
                        layoutType = layoutType,
                        viewState = viewState,
                        viewModel = viewModel,
                    )
                }

                AnnotationType.ink -> {
                    PdfAnnotationMoreInkRow(
                        viewModel = viewModel,
                        viewState = viewState,
                        layoutType = layoutType,
                    )
                }

                AnnotationType.image -> {
                    PdfAnnotationMoreImageRow(
                        viewState = viewState,
                        viewModel = viewModel,
                    )
                }

                AnnotationType.text-> {
                    PdfAnnotationMoreFreeTextRow(
                        viewModel = viewModel,
                        viewState = viewState,
                    )
                }
                AnnotationType.underline -> {
                    PdfAnnotationMoreUnderlineRow(
                        layoutType = layoutType,
                        viewState = viewState,
                        viewModel = viewModel,
                    )
                }

                null -> {
                    //no-op
                }
            }
        }
        item {
            SpacerDivider()
            PageButton(viewModel = viewModel, viewState = viewState)
            SpacerDivider()
            DeleteButton(viewModel = viewModel)
            MoreSidebarDivider(
                modifier = Modifier
                    .height(1.dp)
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
private fun DeleteButton(viewModel: PdfAnnotationMoreViewModel) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(CustomTheme.colors.zoteroEditFieldBackground)
            .safeClickable(
                onClick = viewModel::onDeleteAnnotation,
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true)
            ), contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(Strings.pdf_annotation_popover_delete),
            color = CustomPalette.ErrorRed,
            style = CustomTheme.typography.newBody,
        )
    }
}

@Composable
private fun PageButton(
    viewModel: PdfAnnotationMoreViewModel,
    viewState: PdfAnnotationMoreViewState
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(CustomTheme.colors.zoteroEditFieldBackground)
            .safeClickable(
                onClick = viewModel::onPageClicked,
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true)
            ), contentAlignment = Alignment.CenterStart
    ) {
        Text(
            modifier = Modifier.padding(start = 16.dp),
            text = stringResource(Strings.page) + " " + viewState.pageLabel,
            color = CustomTheme.colors.primaryContent,
            style = CustomTheme.typography.newBody,
        )
    }


//    HeadingTextButton(
//        modifier = Modifier
//            .fillMaxWidth()
//            .background(CustomTheme.colors.zoteroEditFieldBackground),
//        onClick = viewModel::onPageClicked,
//        contentColor = CustomTheme.colors.primaryContent,
//        text = stringResource(Strings.page) + " " + viewState.pageLabel,
//        style = CustomTheme.typography.newBody,
//    )
}

@Composable
internal fun SpacerDivider() {
    MoreSidebarDivider(
        modifier = Modifier
            .height(1.dp)
            .fillMaxWidth()
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(CustomTheme.colors.pdfAnnotationsFormBackground)
    )
    MoreSidebarDivider(
        modifier = Modifier
            .height(1.dp)
            .fillMaxWidth()
    )
}

@Composable
internal fun MoreSidebarDivider(modifier: Modifier = Modifier) {
    Divider(
        modifier = modifier,
        color = CustomTheme.colors.pdfEditAnnotationDividerBackground,
        thickness = 1.dp
    )

}

package org.zotero.android.pdf.annotationmore

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.database.objects.AnnotationType
import org.zotero.android.pdf.annotationmore.data.PdfAnnotationMoreArgs
import org.zotero.android.pdf.annotationmore.rows.PdfAnnotationMoreFreeTextRow
import org.zotero.android.pdf.annotationmore.rows.PdfAnnotationMoreHighlightRow
import org.zotero.android.pdf.annotationmore.rows.PdfAnnotationMoreImageRow
import org.zotero.android.pdf.annotationmore.rows.PdfAnnotationMoreInkRow
import org.zotero.android.pdf.annotationmore.rows.PdfAnnotationMoreNoteRow
import org.zotero.android.pdf.annotationmore.rows.PdfAnnotationMoreUnderlineRow
import org.zotero.android.screens.settings.elements.NewSettingsDivider
import org.zotero.android.uicomponents.CustomScaffoldM3
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.themem3.AppThemeM3

@Composable
internal fun PdfAnnotationMoreScreen(
    args: PdfAnnotationMoreArgs,
    viewModel: PdfAnnotationMoreViewModel = hiltViewModel(),
    navigateToPageEdit: () -> Unit,
    onBack: () -> Unit,
) {

    LaunchedEffect(args) {
        viewModel.init(args = args)
    }
    viewModel.setOsTheme(isDark = isSystemInDarkTheme())
    val viewState by viewModel.viewStates.observeAsState(PdfAnnotationMoreViewState())
    val viewEffect by viewModel.viewEffects.observeAsState()
    AppThemeM3 {
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

        CustomScaffoldM3(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                PdfAnnotationMoreTopBar(onBack = onBack, viewModel = viewModel)
            },
            bottomBar = {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .fillMaxWidth()
                        .windowInsetsPadding(BottomAppBarDefaults.windowInsets)
                )
            }
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
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            PageButton(viewModel = viewModel, viewState = viewState)
            NewSettingsDivider()
        }
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
                        viewState = viewState,
                        viewModel = viewModel,
                    )
                }

                AnnotationType.ink -> {
                    PdfAnnotationMoreInkRow(
                        viewModel = viewModel,
                        viewState = viewState,
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
            NewSettingsDivider()
            DeleteButton(viewModel = viewModel)
        }
    }
}

@Composable
private fun DeleteButton(viewModel: PdfAnnotationMoreViewModel) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .safeClickable(
                onClick = viewModel::onDeleteAnnotation,
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

@Composable
private fun PageButton(
    viewModel: PdfAnnotationMoreViewModel,
    viewState: PdfAnnotationMoreViewState
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .safeClickable(
                onClick = viewModel::onPageClicked,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true)
            ), verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.padding(start = 16.dp),
            text = stringResource(Strings.page_number),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )

        Text(
            modifier = Modifier.padding(start = 16.dp),
            text = viewState.pageLabel,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }

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

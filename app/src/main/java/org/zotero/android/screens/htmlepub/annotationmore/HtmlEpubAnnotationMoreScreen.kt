package org.zotero.android.screens.htmlepub.annotationmore

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.BottomAppBarDefaults
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.database.objects.AnnotationType
import org.zotero.android.screens.htmlepub.annotationmore.data.HtmlEpubAnnotationMoreArgs
import org.zotero.android.screens.settings.elements.NewSettingsDivider
import org.zotero.android.uicomponents.CustomScaffoldM3
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.themem3.AppThemeM3

@Composable
internal fun HtmlEpubAnnotationMoreScreen(
    args: HtmlEpubAnnotationMoreArgs,
    viewModel: HtmlEpubAnnotationMoreViewModel = hiltViewModel(),
    navigateToPageEdit: () -> Unit,
    onBack: () -> Unit,
) {

    LaunchedEffect(args) {
        viewModel.init(args = args)
    }
    viewModel.setOsTheme(isDark = isSystemInDarkTheme())
    val viewState by viewModel.viewStates.observeAsState(HtmlEpubAnnotationMoreViewState())
    val viewEffect by viewModel.viewEffects.observeAsState()
    AppThemeM3 {
        LaunchedEffect(key1 = viewEffect) {
            when (viewEffect?.consume()) {
                is HtmlEpubAnnotationMoreViewEffect.NavigateToPageEditScreen -> {
                    navigateToPageEdit()
                }

                is HtmlEpubAnnotationMoreViewEffect.Back -> {
                    onBack()
                }

                else -> {}
            }
        }

        CustomScaffoldM3(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                HtmlEpubAnnotationMoreTopBar(onBack = onBack, viewModel = viewModel)
            },
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(BottomAppBarDefaults.windowInsets)
                )
            }
        ) {
            HtmlEpubAnnotationMorePart(
                viewState = viewState,
                viewModel = viewModel,
            )
        }
    }
}

@Composable
internal fun HtmlEpubAnnotationMorePart(
    viewState: HtmlEpubAnnotationMoreViewState,
    viewModel: HtmlEpubAnnotationMoreViewModel,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            HtmlEpubAnnotationMorePageButton(viewModel = viewModel, viewState = viewState)
            NewSettingsDivider()
        }
        item {
            when (viewState.type) {
                AnnotationType.note -> {
                    HtmlEpubAnnotationMoreNoteRow(
                        viewModel = viewModel,
                        viewState = viewState,
                    )
                }

                AnnotationType.highlight -> {
                    HtmlEpubAnnotationMoreHighlightRow(
                        viewState = viewState,
                        viewModel = viewModel,
                    )
                }

                AnnotationType.underline -> {
                    HtmlEpubAnnotationMoreUnderlineRow(
                        viewState = viewState,
                        viewModel = viewModel,
                    )
                }

                null -> {
                    //no-op
                }

                else -> {
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
private fun DeleteButton(viewModel: HtmlEpubAnnotationMoreViewModel) {
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
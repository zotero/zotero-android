package org.zotero.android.screens.htmlepub.reader

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalView
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.uicomponents.themem3.AppThemeM3

@Composable
internal fun HtmlEpubReaderScreen(
    onBack: () -> Unit,
    navigateToPdfFilter: () -> Unit,
    navigateToTagPicker: () -> Unit,
    navigateToHtmlEpubAnnotation: () -> Unit,
    navigateToHtmlEpubAnnotationMore: () -> Unit,
    navigateToHtmlEpubColorPicker: () -> Unit,
    navigateToHtmlEpubSettings: (args: String) -> Unit,
    onOpenWebpage: (url: String) -> Unit,
    viewModel: HtmlEpubReaderViewModel = hiltViewModel(),
) {
    viewModel.setOsTheme(isDark = isSystemInDarkTheme())
    val viewState by viewModel.viewStates.observeAsState(HtmlEpubReaderViewState())
    val viewEffect by viewModel.viewEffects.observeAsState()
    val activity = LocalActivity.current ?: return
    val currentView = LocalView.current

    AppThemeM3(darkTheme = viewState.isDark) {
        val annotationsLazyListState = rememberLazyListState()
        val layoutType = CustomLayoutSize.calculateLayoutType()
        LaunchedEffect(key1 = viewEffect) {
            when (val consumedEffect = viewEffect?.consume()) {
                is HtmlEpubReaderViewEffect.NavigateBack -> {
                    onBack()
                }

                is HtmlEpubReaderViewEffect.DisableForceScreenOn -> {
                    currentView.keepScreenOn = false
                }

                is HtmlEpubReaderViewEffect.EnableForceScreenOn -> {
                    currentView.keepScreenOn = true
                }
                is HtmlEpubReaderViewEffect.ShowPdfFilters -> {
                    navigateToPdfFilter()
                }
                is HtmlEpubReaderViewEffect.NavigateToTagPickerScreen -> {
                    navigateToTagPicker()
                }

                is HtmlEpubReaderViewEffect.ShowHtmlEpubAnnotationMore -> {
                    if (layoutType.isTablet()) {
                        navigateToHtmlEpubAnnotationMore()
                    }
                }

                is HtmlEpubReaderViewEffect.ShowHtmlEpubColorPicker -> {
                    navigateToHtmlEpubColorPicker()
                }
                is HtmlEpubReaderViewEffect.ShowHtmlEpubSettings -> {
                    navigateToHtmlEpubSettings(consumedEffect.params)
                }

                is HtmlEpubReaderViewEffect.ShowPdfAnnotationAndUpdateAnnotationsList -> {
                    if (consumedEffect.showAnnotationPopup) {
                        if (layoutType.isTablet()) {
                            navigateToHtmlEpubAnnotation()
                        }
                    }
                    if (consumedEffect.scrollToIndex != -1) {
                        annotationsLazyListState.animateScrollToItem(index = consumedEffect.scrollToIndex)
                    }
                }

                is HtmlEpubReaderViewEffect.ScrollSideBar -> {
                    annotationsLazyListState.scrollToItem(index = consumedEffect.scrollToIndex)
                }

                is HtmlEpubReaderViewEffect.OpenWebpage -> {
                    onOpenWebpage(consumedEffect.url)
                }

                else -> {}
            }
        }

        HtmlEpubReaderWebView(viewModel)

    }

}
package org.zotero.android.pdf.reader

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.architecture.ui.ObserveLifecycleEvent
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.theme.CustomThemeWithStatusAndNavBars

@Composable
internal fun PdfReaderScreen(
    onBack: () -> Unit,
    navigateToPdfFilter: () -> Unit,
    navigateToPdfSettings: () -> Unit,
    navigateToPdfPlainReader: () -> Unit,
    navigateToPdfColorPicker: () -> Unit,
    navigateToPdfAnnotation: () -> Unit,
    navigateToPdfAnnotationMore: () -> Unit,
    navigateToTagPicker: () -> Unit,
    viewModel: PdfReaderViewModel = hiltViewModel(),
) {
    viewModel.setOsTheme(isDark = isSystemInDarkTheme())
    val viewState by viewModel.viewStates.observeAsState(PdfReaderViewState())
    val viewEffect by viewModel.viewEffects.observeAsState()
    val activity = LocalContext.current as? AppCompatActivity ?: return
    ObserveLifecycleEvent { event ->
        when (event) {
            Lifecycle.Event.ON_STOP -> { viewModel.onStop(activity.isChangingConfigurations) }
            else -> {}
        }
    }
    CustomThemeWithStatusAndNavBars(isDarkTheme = viewState.isDark) {
        val window = activity.window
        val decorView = window.decorView
        val systemBars = WindowInsetsCompat.Type.systemBars()
        val insetsController = WindowCompat.getInsetsController(window, decorView)
        if (viewState.isTopBarVisible) {
            insetsController.show(systemBars)
        } else {
            insetsController.hide(systemBars)
        }

        val params = viewModel.screenArgs
        val uri = params.uri
        val annotationsLazyListState = rememberLazyListState()
        val thumbnailsLazyListState = rememberLazyListState()
        val layoutType = CustomLayoutSize.calculateLayoutType()
        val focusManager = LocalFocusManager.current
        LaunchedEffect(key1 = viewEffect) {
            when (val consumedEffect = viewEffect?.consume()) {
                is PdfReaderViewEffect.NavigateBack -> {
                    onBack()
                }

                is PdfReaderViewEffect.ShowPdfFilters -> {
                    navigateToPdfFilter()
                }

                is PdfReaderViewEffect.ShowPdfAnnotationAndUpdateAnnotationsList -> {
                    if (consumedEffect.showAnnotationPopup) {
                        if (!layoutType.isTablet()) {
                            viewModel.removeFragment()
                        }
                        navigateToPdfAnnotation()
                    }
                    if (consumedEffect.scrollToIndex != -1) {
                        annotationsLazyListState.animateScrollToItem(index = consumedEffect.scrollToIndex)
                    }
                }

                is PdfReaderViewEffect.ScrollThumbnailListToIndex -> {
                    val visibleItemsInfo = thumbnailsLazyListState.layoutInfo.visibleItemsInfo
                    val scrollToIndex = consumedEffect.scrollToIndex
                    if (visibleItemsInfo.isNotEmpty() && (scrollToIndex < visibleItemsInfo.first().index || scrollToIndex > visibleItemsInfo.last().index)) {
                        thumbnailsLazyListState.animateScrollToItem(index = scrollToIndex)
                    }
                }

                is PdfReaderViewEffect.ShowPdfAnnotationMore -> {
                    if (!layoutType.isTablet()) {
                        viewModel.removeFragment()
                    }
                    navigateToPdfAnnotationMore()
                }

                is PdfReaderViewEffect.ShowPdfSettings -> {
                    if (!layoutType.isTablet()) {
                        viewModel.removeFragment()
                    }
                    navigateToPdfSettings()
                }

                is PdfReaderViewEffect.ShowPdfPlainReader -> {
                    viewModel.removeFragment()
                    navigateToPdfPlainReader()
                }
                is PdfReaderViewEffect.ShowPdfColorPicker -> {
                    if (!layoutType.isTablet()) {
                        viewModel.removeFragment()
                    }
                    navigateToPdfColorPicker()
                }
                is PdfReaderViewEffect.ClearFocus -> {
                    focusManager.clearFocus()
                }
                is PdfReaderViewEffect.NavigateToTagPickerScreen -> {
                    navigateToTagPicker()
                }
                else -> {}
            }
        }

        CustomScaffold(
            backgroundColor = CustomTheme.colors.pdfAnnotationsTopbarBackground,
            topBar = {
                AnimatedContent(targetState = viewState.isTopBarVisible, label = "") { isTopBarVisible ->
                    if (isTopBarVisible) {
                        PdfReaderTopBar(
                            onBack = onBack,
                            onShowHideSideBar = viewModel::toggleSideBar,
                            toPdfSettings = viewModel::navigateToPdfSettings,
                            toPdfPlainReader = viewModel::navigateToPlainReader,
                            showPdfSearch = viewState.showPdfSearch,
                            toggleToolbarButton = viewModel::toggleToolbarButton,
                            isToolbarButtonSelected = viewState.showCreationToolbar,
                            showSideBar = viewState.showSideBar,
                            onShowHidePdfSearch = viewModel::togglePdfSearch,
                            viewModel = viewModel,
                            viewState = viewState
                        )
                    }
                }

            },
        ) {
            if (layoutType.isTablet()) {
                PdfReaderTabletMode(
                    vMInterface = viewModel,
                    viewState = viewState,
                    annotationsLazyListState = annotationsLazyListState,
                    thumbnailsLazyListState = thumbnailsLazyListState,
                    layoutType = layoutType,
                    uri = uri,
                )
            } else {
                PdfReaderPhoneMode(
                    viewState = viewState,
                    vMInterface = viewModel,
                    annotationsLazyListState = annotationsLazyListState,
                    thumbnailsLazyListState = thumbnailsLazyListState,
                    layoutType = layoutType,
                    uri = uri,
                )
            }
        }
    }

}


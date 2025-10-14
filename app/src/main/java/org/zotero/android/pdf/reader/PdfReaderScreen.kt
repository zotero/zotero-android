package org.zotero.android.pdf.reader

import android.view.MotionEvent
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.architecture.ui.ObserveLifecycleEvent
import org.zotero.android.pdf.annotation.sidebar.PdfAnnotationNavigationView
import org.zotero.android.pdf.annotationmore.sidebar.PdfAnnotationMoreNavigationView
import org.zotero.android.pdf.reader.modes.PdfReaderPhoneMode
import org.zotero.android.pdf.reader.modes.PdfReaderTabletMode
import org.zotero.android.pdf.reader.pdfsearch.PdfReaderSearchViewModel
import org.zotero.android.pdf.reader.pdfsearch.PdfReaderSearchViewState
import org.zotero.android.pdf.reader.topbar.PdfReaderSearchTopBar
import org.zotero.android.pdf.reader.topbar.PdfReaderTopBar
import org.zotero.android.pdf.settings.sidebar.PdfCopyCitationView
import org.zotero.android.pdf.settings.sidebar.PdfSettingsView
import org.zotero.android.uicomponents.CustomScaffoldM3
import org.zotero.android.uicomponents.themem3.AppThemeM3
import java.io.File

@Composable
internal fun PdfReaderScreen(
    onBack: () -> Unit,
    onExportPdf: (file: File) -> Unit,
    navigateToPdfFilter: () -> Unit,
    navigateToPdfSettings: (args: String) -> Unit,
    navigateToPdfPlainReader: (args: String) -> Unit,
    navigateToPdfColorPicker: () -> Unit,
    navigateToPdfAnnotation: () -> Unit,
    navigateToPdfAnnotationMore: () -> Unit,
    navigateToTagPicker: () -> Unit,
    navigateToSingleCitationScreen: () -> Unit,
    viewModel: PdfReaderViewModel = hiltViewModel(),
) {
    viewModel.setOsTheme(isDark = isSystemInDarkTheme())
    val viewState by viewModel.viewStates.observeAsState(PdfReaderViewState())
    val viewEffect by viewModel.viewEffects.observeAsState()
    val activity = LocalActivity.current ?: return
    val currentView = LocalView.current
    ObserveLifecycleEvent { event ->
        when (event) {
            Lifecycle.Event.ON_STOP -> {
                currentView.keepScreenOn = false
                viewModel.onStop(activity.isChangingConfigurations)
            }

            else -> {}
        }
    }
    AppThemeM3(darkTheme = viewState.isDark) {
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

                is PdfReaderViewEffect.DisableForceScreenOn -> {
                    currentView.keepScreenOn = false
                }

                is PdfReaderViewEffect.EnableForceScreenOn -> {
                    currentView.keepScreenOn = true
                }

                is PdfReaderViewEffect.ShowPdfFilters -> {
                    navigateToPdfFilter()
                }

                is PdfReaderViewEffect.ScrollSideBar -> {
                    annotationsLazyListState.scrollToItem(index = consumedEffect.scrollToIndex)
                }

                is PdfReaderViewEffect.ShowPdfAnnotationAndUpdateAnnotationsList -> {
                    if (consumedEffect.showAnnotationPopup) {
                        if (layoutType.isTablet()) {
                            navigateToPdfAnnotation()
                        }
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
                    if (layoutType.isTablet()) {
                        navigateToPdfAnnotationMore()
                    }
                }

                is PdfReaderViewEffect.ShowPdfSettings -> {
                    if (!layoutType.isTablet()) {
                        viewModel.removeFragment()
                    }
                    navigateToPdfSettings(consumedEffect.params)
                }

                is PdfReaderViewEffect.ShowSingleCitationScreen -> {
                    if (!layoutType.isTablet()) {
                        viewModel.removeFragment()
                    }
                    navigateToSingleCitationScreen()
                }

                is PdfReaderViewEffect.ShowPdfPlainReader -> {
                    viewModel.removeFragment()
                    navigateToPdfPlainReader(consumedEffect.params)
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

                is PdfReaderViewEffect.ExportPdf -> {
                    onExportPdf(consumedEffect.file)
                }

                else -> {}
            }
        }

        val pdfReaderSearchViewModel: PdfReaderSearchViewModel = hiltViewModel()
        val pdfReaderSearchViewState by pdfReaderSearchViewModel.viewStates.observeAsState(
            PdfReaderSearchViewState()
        )

        CustomScaffoldM3(
            modifier = Modifier.pointerInteropFilter {
                when (it.action) {
                    MotionEvent.ACTION_DOWN -> viewModel.restartDisableForceScreenOnTimer()
                }
                false
            },
            topBar = {
                AnimatedContent(
                    targetState = viewState.isTopBarVisible,
                    label = ""
                ) { isTopBarVisible ->
                    if (isTopBarVisible) {
                        if (viewState.showPdfSearch && !layoutType.isTablet()) {
                            PdfReaderSearchTopBar(
                                viewState = pdfReaderSearchViewState,
                                viewModel = pdfReaderSearchViewModel,
                                togglePdfSearch = viewModel::togglePdfSearch
                            )
                        } else {
                            PdfReaderTopBar(
                                onBack = onBack,
                                onShowHideSideBar = viewModel::toggleSideBar,
                                onShareButtonTapped = viewModel::onShareButtonTapped,
                                toPdfSettings = viewModel::navigateToPdfSettings,
                                toPdfPlainReader = viewModel::navigateToPlainReader,
                                showPdfSearch = viewState.showPdfSearch,
                                toggleToolbarButton = viewModel::toggleToolbarButton,
                                isToolbarButtonSelected = viewState.showCreationToolbar,
                                showSideBar = viewState.showSideBar,
                                onShowHidePdfSearch = viewModel::togglePdfSearch,
                                viewModel = viewModel,
                                viewState = viewState,
                                pdfReaderSearchViewState = pdfReaderSearchViewState,
                                pdfReaderSearchViewModel = pdfReaderSearchViewModel,
                            )
                        }
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
                    pdfReaderSearchViewModel = pdfReaderSearchViewModel,
                    pdfReaderSearchViewState = pdfReaderSearchViewState,
                    annotationsLazyListState = annotationsLazyListState,
                    thumbnailsLazyListState = thumbnailsLazyListState,
                    layoutType = layoutType,
                    uri = uri,
                )
            }
        }
        PdfAnnotationNavigationView(viewState = viewState, viewModel = viewModel)
        PdfAnnotationMoreNavigationView(viewState = viewState, viewModel = viewModel)
        PdfSettingsView(viewState = viewState, viewModel = viewModel)
        PdfCopyCitationView(viewState = viewState, viewModel = viewModel)
    }

}
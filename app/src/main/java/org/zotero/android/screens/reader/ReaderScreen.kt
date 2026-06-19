package org.zotero.android.screens.reader

import android.content.res.Resources
import android.util.TypedValue
import android.view.MotionEvent
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import org.zotero.android.R
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.architecture.ui.ObserveLifecycleEvent
import org.zotero.android.screens.reader.annotation.ReaderAnnotationNavigationView
import org.zotero.android.screens.reader.annotationmore.ReaderAnnotationMoreNavigationView
import org.zotero.android.screens.reader.colorpicker.ReaderColorPickerView
import org.zotero.android.screens.reader.data.ReaderFileType
import org.zotero.android.screens.reader.filter.ReaderFilterView
import org.zotero.android.screens.reader.search.ReaderSearchScreen
import org.zotero.android.screens.reader.search.ReaderSearchViewModel
import org.zotero.android.screens.reader.search.ReaderSearchViewState
import org.zotero.android.screens.reader.settings.ReaderSettingsView
import org.zotero.android.screens.reader.sidebar.thumbnails.ReaderThumbnailsViewModel
import org.zotero.android.screens.reader.topbar.ReaderSearchTopBar
import org.zotero.android.screens.reader.topbar.ReaderTopBar
import org.zotero.android.uicomponents.CustomScaffoldM3
import org.zotero.android.uicomponents.themem3.AppThemeM3
import timber.log.Timber

@Composable
internal fun ReaderScreen(
    onBack: () -> Unit,
    navigateToPdfFilter: () -> Unit,
    navigateToTagPicker: () -> Unit,
    navigateToReaderAnnotation: () -> Unit,
    navigateToReaderAnnotationMore: () -> Unit,
    navigateToReaderColorPicker: () -> Unit,
    navigateToReaderSettings: (args: String) -> Unit,
    onOpenWebpage: (url: String) -> Unit,
    viewModel: ReaderViewModel = hiltViewModel(),
) {
    viewModel.setOsTheme(isDark = isSystemInDarkTheme())
    val viewState by viewModel.viewStates.observeAsState(ReaderViewState())
    val viewEffect by viewModel.viewEffects.observeAsState()

    val thumbnailsViewModel: ReaderThumbnailsViewModel = hiltViewModel()
    thumbnailsViewModel.initOnce()

    val activity = LocalActivity.current ?: return
    val currentView = LocalView.current

    ObserveLifecycleEvent { event ->
        when (event) {
            Lifecycle.Event.ON_STOP -> {
                currentView.keepScreenOn = false
                viewModel.onStop()
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

        val annotationMaxSideSize = annotationMaxSideSize()

        val focusManager = LocalFocusManager.current
        val annotationsLazyListState = rememberLazyListState()
        val layoutType = CustomLayoutSize.calculateLayoutType()
        LaunchedEffect(key1 = viewEffect) {
            when (val consumedEffect = viewEffect?.consume()) {
                is ReaderViewEffect.NavigateBack -> {
                    onBack()
                }

                is ReaderViewEffect.DisableForceScreenOn -> {
                    currentView.keepScreenOn = false
                }

                is ReaderViewEffect.EnableForceScreenOn -> {
                    currentView.keepScreenOn = true
                }

                is ReaderViewEffect.ShowPdfFilters -> {
                    navigateToPdfFilter()
                }

                is ReaderViewEffect.NavigateToTagPickerScreen -> {
                    navigateToTagPicker()
                }

                is ReaderViewEffect.ShowReaderAnnotationMore -> {
                    if (layoutType.isTablet()) {
                        navigateToReaderAnnotationMore()
                    }
                }

                is ReaderViewEffect.ShowReaderColorPicker -> {
                    navigateToReaderColorPicker()
                }

                is ReaderViewEffect.ShowReaderSettings -> {
                    navigateToReaderSettings(consumedEffect.params)
                }

                is ReaderViewEffect.ShowPdfAnnotationAndUpdateAnnotationsList -> {
                    if (consumedEffect.showAnnotationPopup) {
                        if (layoutType.isTablet()) {
                            navigateToReaderAnnotation()
                        }
                    }
                    if (consumedEffect.scrollToIndex != -1) {
                        annotationsLazyListState.animateScrollToItem(index = consumedEffect.scrollToIndex)
                    }
                }

                is ReaderViewEffect.ScrollSideBar -> {
                    annotationsLazyListState.scrollToItem(index = consumedEffect.scrollToIndex)
                }

                is ReaderViewEffect.OpenWebpage -> {
                    onOpenWebpage(consumedEffect.url)
                }

                is ReaderViewEffect.OnPageChanged -> {
                    thumbnailsViewModel.onPageChangedByReader(consumedEffect.currentPage)
                }

                else -> {
                    //no-op
                }
            }
        }

        val readerSearchViewModel: ReaderSearchViewModel = hiltViewModel()
        val readerSearchViewState by readerSearchViewModel.viewStates.observeAsState(
            ReaderSearchViewState()
        )

        CustomScaffoldM3(
            modifier = Modifier
                .pointerInteropFilter {
                    when (it.action) {
                        MotionEvent.ACTION_DOWN -> {
                            viewModel.restartDisableForceScreenOnTimer()
                        }
                    }
                    false
                },
            shouldIncludeTopBarAndNavBarPaddings = viewState.isPdfOrHtml(),
            topBar = {
                AnimatedContent(
                    targetState = viewState.isTopBarVisible,
                    label = ""
                ) { isTopBarVisible ->
                    if (isTopBarVisible) {
                        if (viewState.showPdfSearch && !layoutType.isTablet()) {
                            ReaderSearchTopBar(
                                viewState = readerSearchViewState,
                                viewModel = readerSearchViewModel,
                                togglePdfSearch = viewModel::togglePdfSearch
                            )
                        } else {
                            ReaderTopBar(
                                onBack = onBack,
                                onShowHideSideBar = viewModel::toggleSideBar,
                                toPdfSettings = viewModel::navigateToReaderSettings,
                                showPdfSearch = viewState.showPdfSearch,
                                toggleToolbarButton = viewModel::toggleToolbarButton,
                                isToolbarButtonSelected = viewState.showCreationToolbar,
                                showSideBar = viewState.showSideBar,
                                onShowHidePdfSearch = viewModel::togglePdfSearch,
                                viewModel = viewModel,
                                viewState = viewState,
                                readerSearchViewState = readerSearchViewState,
                                readerSearchViewModel = readerSearchViewModel,
                            )
                        }
                    }
                }

            },
        ) {
            when (viewState.fileType) {
                ReaderFileType.PDF, ReaderFileType.HTML -> {
                    if (layoutType.isTablet()) {
                        ReaderSideBySideMode(
                            viewModel = viewModel,
                            viewState = viewState,
                            annotationsLazyListState = annotationsLazyListState,
                            annotationMaxSideSize = annotationMaxSideSize
                        )
                    } else {
                        ReaderOverlayMode(
                            viewState = viewState,
                            viewModel = viewModel,
                            annotationsLazyListState = annotationsLazyListState,
                            layoutType = layoutType,
                            annotationMaxSideSize = annotationMaxSideSize
                        )
                    }
                }

                ReaderFileType.EPUB -> {
                    ReaderOverlayMode(
                        viewState = viewState,
                        viewModel = viewModel,
                        annotationsLazyListState = annotationsLazyListState,
                        layoutType = layoutType,
                        annotationMaxSideSize = annotationMaxSideSize
                    )
                }

            }
        }
        if (!layoutType.isTablet()) {
            AnimatedContent(
                targetState = viewState.showPdfSearch,
                transitionSpec = {
                    readerPdfSearchTransitionSpec()
                },
                label = ""
            ) { showScreen ->
                if (showScreen) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(TopAppBarDefaults.windowInsets)
                            .padding(top = TopAppBarDefaults.TopAppBarExpandedHeight)
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        ReaderSearchScreen(
                            onBack = viewModel::hidePdfSearch,
                            viewModel = readerSearchViewModel,
                            viewState = readerSearchViewState,
                        )
                    }
                }
            }
        }
        ReaderAnnotationMoreNavigationView(viewState = viewState, viewModel = viewModel)
        ReaderAnnotationNavigationView(viewState = viewState, viewModel = viewModel)
        ReaderSettingsView(viewState = viewState, viewModel = viewModel)
        ReaderColorPickerView(viewState = viewState, viewModel = viewModel)
        ReaderFilterView(viewState = viewState, viewModel = viewModel)
    }

}

@Composable
private fun annotationMaxSideSize(): Int {
    val layoutType = CustomLayoutSize.calculateLayoutType()
    val context = LocalContext.current
    val outValue = TypedValue()
    context.resources.getValue(R.dimen.pdf_sidebar_width_percent, outValue, true)
    val sidebarWidthPercentage = outValue.float
    val metricsWidthPixels = Resources.getSystem().displayMetrics.widthPixels
    val annotationSize = metricsWidthPixels * sidebarWidthPercentage
    val result = annotationSize.toInt()
    if (result <= 0) {
        val errorMessage = "ReaderWebView annotationMaxSideSize is $result" +
                ".sidebarWidthPercentage = $sidebarWidthPercentage" +
                ".metricsWidthPixels = $metricsWidthPixels"
        Timber.e(errorMessage)
        return if (layoutType.isTablet()) {
            480
        } else {
            1080
        }
    }
    return result
}
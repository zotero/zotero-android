package org.zotero.android.screens.htmlepub.reader

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
import org.zotero.android.screens.htmlepub.reader.search.HtmlEpubReaderSearchViewModel
import org.zotero.android.screens.htmlepub.reader.search.HtmlEpubReaderSearchViewState
import org.zotero.android.screens.htmlepub.reader.topbar.HtmlEpubReaderSearchTopBar
import org.zotero.android.screens.htmlepub.reader.topbar.HtmlEpubReaderTopBar
import org.zotero.android.uicomponents.CustomScaffoldM3
import org.zotero.android.uicomponents.themem3.AppThemeM3

@Composable
internal fun HtmlEpubReaderScreen(
    onBack: () -> Unit,
    viewModel: HtmlEpubReaderViewModel = hiltViewModel(),
) {
    viewModel.setOsTheme(isDark = isSystemInDarkTheme())
    val viewState by viewModel.viewStates.observeAsState(HtmlEpubReaderViewState())
    val viewEffect by viewModel.viewEffects.observeAsState()
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

        val focusManager = LocalFocusManager.current
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

                else -> {}
            }
        }

        val htmlEpubReaderSearchViewModel: HtmlEpubReaderSearchViewModel = hiltViewModel()
        val htmlEpubReaderSearchViewState by htmlEpubReaderSearchViewModel.viewStates.observeAsState(
            HtmlEpubReaderSearchViewState()
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
                            HtmlEpubReaderSearchTopBar(
                                viewState = htmlEpubReaderSearchViewState,
                                viewModel = htmlEpubReaderSearchViewModel,
                                togglePdfSearch = viewModel::togglePdfSearch
                            )
                        } else {
                            HtmlEpubReaderTopBar(
                                onBack = onBack,
                                onShowHideSideBar = viewModel::toggleSideBar,
                                toPdfSettings = viewModel::navigateToPdfSettings,
                                showPdfSearch = viewState.showPdfSearch,
                                toggleToolbarButton = viewModel::toggleToolbarButton,
                                isToolbarButtonSelected = viewState.showCreationToolbar,
                                showSideBar = viewState.showSideBar,
                                onShowHidePdfSearch = viewModel::togglePdfSearch,
                                viewModel = viewModel,
                                viewState = viewState,
                                htmlEpubReaderSearchViewState = htmlEpubReaderSearchViewState,
                                htmlEpubReaderSearchViewModel = htmlEpubReaderSearchViewModel,
                            )
                        }
                    }
                }

            },
        ) {
            if (layoutType.isTablet()) {
                //TODO
            } else {
                HtmlEpubReaderPhoneMode(
                    viewState = viewState,
                    viewModel = viewModel,
                    htmlEpubReaderSearchViewModel = htmlEpubReaderSearchViewModel,
                    htmlEpubReaderSearchViewState = htmlEpubReaderSearchViewState,
                    layoutType = layoutType,
                    annotationsLazyListState = annotationsLazyListState
                )
            }
        }
    }

}
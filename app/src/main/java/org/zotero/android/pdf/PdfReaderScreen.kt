package org.zotero.android.pdf

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.theme.CustomThemeWithStatusAndNavBars

@Composable
internal fun PdfReaderScreen(
    onBack: () -> Unit,
    navigateToPdfFilter: () -> Unit,
    navigateToPdfSettings: () -> Unit,
    viewModel: PdfReaderViewModel = hiltViewModel(),
) {
    viewModel.setOsTheme(isDark = isSystemInDarkTheme())
    val viewState by viewModel.viewStates.observeAsState(PdfReaderViewState())
    val viewEffect by viewModel.viewEffects.observeAsState()
    ObserveLifecycleEvent { event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> { viewModel.onResume() }
            else -> {}
        }
    }
    CustomThemeWithStatusAndNavBars(isDarkTheme = viewState.isDark) {
        LockScreenOrientation()
        val params = ScreenArguments.pdfReaderArgs
        val uri = params.uri


        val lazyListState = rememberLazyListState()


        LaunchedEffect(key1 = viewEffect) {
            when (val consumedEffect = viewEffect?.consume()) {
                PdfReaderViewEffect.NavigateBack -> {
                    onBack()
                }

                PdfReaderViewEffect.ShowPdfFilters -> {
                    navigateToPdfFilter()
                }

                is PdfReaderViewEffect.UpdateAnnotationsList -> {
                    if (consumedEffect.scrollToIndex != -1) {
                        lazyListState.animateScrollToItem(index = consumedEffect.scrollToIndex)
                    }
                }

                is PdfReaderViewEffect.ShowPdfSettings -> {
                    navigateToPdfSettings()
                }

                null -> {

                }
            }
        }

        val layoutType = CustomLayoutSize.calculateLayoutType()
//    var showSideBar by remember { mutableStateOf(false) }

        CustomScaffold(
            backgroundColor = CustomTheme.colors.pdfAnnotationsTopbarBackground,
            topBar = {
                PdfReaderTopBar(
                    onShowHideSideBar = {
                        viewModel.toggleSideBar()
                    },
                    toPdfSettings = { viewModel.navigateToPdfSettings() }
                )
            },
        ) {
            if (layoutType.isTablet()) {
                PdfReaderTabletMode(
                    showSideBar = viewState.showSideBar,
                    viewState = viewState,
                    viewModel = viewModel,
                    lazyListState = lazyListState,
                    layoutType = layoutType,
                    uri = uri
                )
            } else {
                PdfReaderPhoneMode(
                    showSideBar = viewState.showSideBar,
                    viewState = viewState,
                    viewModel = viewModel,
                    lazyListState = lazyListState,
                    layoutType = layoutType,
                    uri = uri
                )
            }
        }
    }

}

@Composable
private fun PdfReaderTabletMode(
    showSideBar: Boolean,
    viewState: PdfReaderViewState,
    viewModel: PdfReaderViewModel,
    lazyListState: LazyListState,
    layoutType: CustomLayoutSize.LayoutType,
    uri: Uri
) {
    Row(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(targetState = showSideBar, transitionSpec = {
            createSidebarTransitionSpec()
        }) { showSideBar ->
            if (showSideBar) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.3f)
                ) {
                    PdfReaderSidebar(
                        viewState = viewState,
                        viewModel = viewModel,
                        lazyListState = lazyListState,
                        layoutType = layoutType
                    )
                }

            }
        }
        if (showSideBar) {
            SidebarDivider(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight()
            )
        }

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            PdfReaderPspdfKitView(uri = uri, viewModel = viewModel)
        }
    }
}

@Composable
private fun PdfReaderPhoneMode(
    showSideBar: Boolean,
    viewState: PdfReaderViewState,
    viewModel: PdfReaderViewModel,
    lazyListState: LazyListState,
    layoutType: CustomLayoutSize.LayoutType,
    uri: Uri
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            PdfReaderPspdfKitView(uri = uri, viewModel = viewModel)
        }
        AnimatedContent(targetState = showSideBar, transitionSpec = {
            createSidebarTransitionSpec()
        }) { showSideBar ->
            if (showSideBar) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CustomTheme.colors.pdfAnnotationsFormBackground)
                ) {
                    PdfReaderSidebar(
                        viewState = viewState,
                        viewModel = viewModel,
                        lazyListState = lazyListState,
                        layoutType = layoutType
                    )
                }
            }
        }
    }
}

private fun AnimatedContentScope<Boolean>.createSidebarTransitionSpec(): ContentTransform {
    val intOffsetSpec = tween<IntOffset>()
    return (slideInHorizontally(intOffsetSpec) { -it } with
            slideOutHorizontally(intOffsetSpec) { -it }).using(
        // Disable clipping since the faded slide-in/out should
        // be displayed out of bounds.
        SizeTransform(
            clip = false,
            sizeAnimationSpec = { _, _ -> tween() }
        ))
}

@Composable
fun LockScreenOrientation() {
    val currentOrientation =
        if (LocalConfiguration.current.orientation == SCREEN_ORIENTATION_PORTRAIT) SCREEN_ORIENTATION_USER_PORTRAIT else SCREEN_ORIENTATION_USER_LANDSCAPE
    val context = LocalContext.current
    DisposableEffect(currentOrientation) {
        val activity = context.findActivity() ?: return@DisposableEffect onDispose {}
        val originalOrientation = activity.requestedOrientation
        activity.requestedOrientation = currentOrientation
        onDispose {
            activity.requestedOrientation = originalOrientation
        }
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun ObserveLifecycleEvent(onEvent: (Lifecycle.Event) -> Unit = {}) {
    // Safely update the current lambdas when a new one is provided
    val currentOnEvent by rememberUpdatedState(onEvent)
    val lifecycleOwner = LocalLifecycleOwner.current

    // If `lifecycleOwner` changes, dispose and reset the effect
    DisposableEffect(lifecycleOwner) {
        // Create an observer that triggers our remembered callbacks
        // for sending analytics events
        val observer = LifecycleEventObserver { _, event ->
            currentOnEvent(event)
        }

        // Add the observer to the lifecycle
        lifecycleOwner.lifecycle.addObserver(observer)

        // When the effect leaves the Composition, remove the observer
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

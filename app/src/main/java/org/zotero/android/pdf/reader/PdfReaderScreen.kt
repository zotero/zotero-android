package org.zotero.android.pdf.reader

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.Modifier
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import org.zotero.android.BuildConfig
import org.zotero.android.R
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
    navigateToPdfPlainReader: (String) -> Unit,
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
    val externalPdfEditLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        viewModel.onExternalPdfEditorReturned()
    }
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
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (viewState.isTopBarVisible) {
            insetsController.show(systemBars)
        } else {
            insetsController.hide(systemBars)
        }

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
                    navigateToPdfPlainReader(consumedEffect.encodedFilPath)
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

                is PdfReaderViewEffect.OpenExternalPdfEditor -> {
                    val uri = FileProvider.getUriForFile(
                        activity,
                        "${BuildConfig.APPLICATION_ID}.provider",
                        consumedEffect.file,
                    )
                    val grantFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    val editIntent = Intent(Intent.ACTION_EDIT).apply {
                        setDataAndType(uri, "application/pdf")
                        putExtra(MediaStore.EXTRA_OUTPUT, uri)
                        clipData = ClipData.newUri(activity.contentResolver, consumedEffect.file.name, uri)
                        addFlags(grantFlags)
                    }
                    if (editIntent.resolveActivity(activity.packageManager) == null) {
                        editIntent.action = Intent.ACTION_VIEW
                    }
                    val chooser = Intent.createChooser(
                        editIntent,
                        activity.getString(R.string.pdf_open_annotated_copy_title),
                    ).apply {
                        addFlags(grantFlags)
                        clipData = editIntent.clipData
                    }
                    try {
                        externalPdfEditLauncher.launch(chooser)
                    } catch (error: ActivityNotFoundException) {
                        viewModel.onExternalPdfEditorLaunchFailed()
                        android.widget.Toast.makeText(
                            activity,
                            R.string.pdf_annotated_copy_prepare_failed,
                            android.widget.Toast.LENGTH_LONG,
                        ).show()
                    } catch (error: Exception) {
                        viewModel.onExternalPdfEditorLaunchFailed()
                        android.widget.Toast.makeText(
                            activity,
                            error.message ?: activity.getString(R.string.pdf_annotated_copy_prepare_failed),
                            android.widget.Toast.LENGTH_LONG,
                        ).show()
                    }
                }

                else -> {}
            }
        }

        val pdfReaderSearchViewModel: PdfReaderSearchViewModel = hiltViewModel()
        val pdfReaderSearchViewState by pdfReaderSearchViewModel.viewStates.observeAsState(
            PdfReaderSearchViewState()
        )

        CustomScaffoldM3(
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
                )
            }
        }
        PdfAnnotationNavigationView(viewState = viewState, viewModel = viewModel)
        PdfAnnotationMoreNavigationView(viewState = viewState, viewModel = viewModel)
        PdfSettingsView(viewState = viewState, viewModel = viewModel)
        PdfCopyCitationView(viewState = viewState, viewModel = viewModel)
    }

}

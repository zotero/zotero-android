package org.zotero.android.pdf.reader.plainreader

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.uicomponents.CustomScaffoldM3
import org.zotero.android.uicomponents.themem3.AppThemeM3

@Composable
internal fun PdfPlainReaderScreen(
    onBack: () -> Unit,
    viewModel: PdfPlainReaderViewModel = hiltViewModel(),
) {
    viewModel.setOsTheme(isDark = isSystemInDarkTheme())
    val viewState by viewModel.viewStates.observeAsState(PdfPlainReaderViewState())
    val viewEffect by viewModel.viewEffects.observeAsState()

    AppThemeM3(darkTheme = viewState.isDark) {
        LaunchedEffect(key1 = viewEffect) {
            when (viewEffect?.consume()) {
                is PdfPlainReaderViewEffect.NavigateBack -> {
                    onBack()
                }

                else -> {}
            }
        }

        CustomScaffoldM3(
            topBar = {
                PdfPlainReaderTopBar(
                    onBack = onBack,
                    title = viewState.title,
                )

            },
        ) {
            PdfPlainReaderPspdfKitView(viewModel)
        }
    }

}


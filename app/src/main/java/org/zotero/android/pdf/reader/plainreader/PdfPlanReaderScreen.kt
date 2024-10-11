package org.zotero.android.pdf.reader.plainreader

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.theme.CustomThemeWithStatusAndNavBars

@Composable
internal fun PdfPlanReaderScreen(
    onBack: () -> Unit,
    viewModel: PdfPlainReaderViewModel = hiltViewModel(),
) {
    viewModel.setOsTheme(isDark = isSystemInDarkTheme())
    val viewState by viewModel.viewStates.observeAsState(PdfPlainReaderViewState())
    val viewEffect by viewModel.viewEffects.observeAsState()

    CustomThemeWithStatusAndNavBars(isDarkTheme = viewState.isDark) {
        LaunchedEffect(key1 = viewEffect) {
            when (val consumedEffect = viewEffect?.consume()) {
                is PdfPlainReaderViewEffect.NavigateBack -> {
                    onBack()
                }

                else -> {}
            }
        }

        CustomScaffold(
            backgroundColor = CustomTheme.colors.pdfAnnotationsTopbarBackground,
            topBar = {
                PdfPlainReaderTopBar(
                    onBack = onBack,
                    title = viewState.title,
                )

            },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                PdfPlainReaderPspdfKitView(viewModel)
            }
        }
    }

}


package org.zotero.android.pdf.annotationmore.editpage

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.textinput.CustomTextField
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.theme.CustomThemeWithStatusAndNavBars

@Composable
internal fun PdfAnnotationEditPageScreen(
    viewModel: PdfAnnotationEditPageViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    viewModel.init()
    viewModel.setOsTheme(isDark = isSystemInDarkTheme())
    val viewState by viewModel.viewStates.observeAsState(PdfAnnotationEditPageViewState())
    val viewEffect by viewModel.viewEffects.observeAsState()
    CustomThemeWithStatusAndNavBars(
        isDarkTheme = viewState.isDark,
        statusBarBackgroundColor = CustomTheme.colors.zoteroEditFieldBackground,
        navBarBackgroundColor = CustomTheme.colors.pdfAnnotationsFormBackground,
    ) {
        LaunchedEffect(key1 = viewEffect) {
            when (viewEffect?.consume()) {
                is PdfAnnotationEditPageViewEffect.Back -> {
                    onBack()
                }

                else -> {}
            }
        }

        CustomScaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                PdfAnnotationEditPageTopBar(viewModel)
            },
        ) {
            PdfAnnotationEditPagePart(
                viewState = viewState,
                viewModel = viewModel,
            )
        }
    }
}

@Composable
private fun PdfAnnotationEditPagePart(
    viewState: PdfAnnotationEditPageViewState,
    viewModel: PdfAnnotationEditPageViewModel,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CustomTheme.colors.pdfAnnotationsFormBackground)
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(CustomTheme.colors.zoteroEditFieldBackground),
            contentAlignment = Alignment.CenterStart
        ) {
            CustomTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp),
                maxCharacters = 16,
                value = viewState.pageLabel,
                hint = "",
                onValueChange = viewModel::onValueChange,
                textStyle = CustomTheme.typography.newBody,
            )
        }
    }
}
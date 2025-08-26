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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.uicomponents.CustomScaffoldM3
import org.zotero.android.uicomponents.textinput.CustomTextField
import org.zotero.android.uicomponents.themem3.AppThemeM3

@Composable
internal fun PdfAnnotationEditPageScreen(
    viewModel: PdfAnnotationEditPageViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    viewModel.init()
    viewModel.setOsTheme(isDark = isSystemInDarkTheme())
    val viewState by viewModel.viewStates.observeAsState(PdfAnnotationEditPageViewState())
    val viewEffect by viewModel.viewEffects.observeAsState()
    AppThemeM3(
        darkTheme = viewState.isDark,
    ) {
        LaunchedEffect(key1 = viewEffect) {
            when (viewEffect?.consume()) {
                is PdfAnnotationEditPageViewEffect.Back -> {
                    onBack()
                }

                else -> {}
            }
        }

        CustomScaffoldM3(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
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
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(MaterialTheme.colorScheme.surface),
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
                textColor = MaterialTheme.colorScheme.onSurface,
                textStyle = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
package org.zotero.android.pdf.pdffilter

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.uicomponents.CustomScaffoldM3
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.theme.CustomPalette
import org.zotero.android.uicomponents.themem3.AppThemeM3

@Composable
internal fun PdfFilterScreen(
    onBack: () -> Unit,
    navigateToTagPicker: () -> Unit,
    viewModel: PdfFilterViewModel = hiltViewModel(),
) {
    viewModel.init()

    val viewState by viewModel.viewStates.observeAsState(PdfFilterViewState())
    val viewEffect by viewModel.viewEffects.observeAsState()
    AppThemeM3(darkTheme = viewState.isDark) {
        LaunchedEffect(key1 = viewEffect) {
            when (viewEffect?.consume()) {
                null -> Unit
                is PdfFilterViewEffect.OnBack -> {
                    onBack()
                }

                is PdfFilterViewEffect.NavigateToTagPickerScreen -> {
                    navigateToTagPicker()
                }
            }
        }
        CustomScaffoldM3(
            topBar = {
                PdfFilterScreenTopBar(
                    onBack = viewModel::onClose,
                    onClear = if (viewState.isClearVisible()) viewModel::onClear else null
                )
            },
        ) {
            Column(
                modifier = Modifier
            ) {
                LazyRow(
                    modifier = Modifier.align(CenterHorizontally),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    items(items = viewState.availableColors) { colorHex ->
                        FilterCircle(
                            hex = colorHex,
                            isSelected = viewState.colors.contains(colorHex),
                            onClick = { viewModel.toggleColor(colorHex) })
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                }
                PdfFilterTagsListAndSelect(
                    viewState = viewState,
                    viewModel = viewModel,
                )
            }
        }
    }
}

@Composable
private fun FilterCircle(hex: String, isSelected: Boolean, onClick: () -> Unit) {
    val color = hex.toColorInt()
    Canvas(modifier = Modifier
        .size(32.dp)
        .safeClickable(onClick = onClick), onDraw = {
        drawCircle(color = Color(color))
        if (isSelected) {
            drawCircle(
                color = CustomPalette.White,
                radius = 12.dp.toPx(),
                style = Stroke(width = 3.dp.toPx())
            )
        }
    })
}
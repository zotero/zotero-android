package org.zotero.android.screens.htmlepub.colorpicker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.uicomponents.CustomScaffoldM3
import org.zotero.android.uicomponents.foundation.debounceClickable
import org.zotero.android.uicomponents.theme.CustomPalette
import org.zotero.android.uicomponents.themem3.AppThemeM3

@Composable
internal fun HtmlEpubReaderColorPickerScreen(
    onBack: () -> Unit,
    viewModel: HtmlEpubReaderColorPickerViewModel = hiltViewModel(),
) {
    viewModel.init()
    viewModel.setOsTheme(isDark = isSystemInDarkTheme())
    val viewState by viewModel.viewStates.observeAsState(HtmlEpubReaderColorPickerViewState())
    val viewEffect by viewModel.viewEffects.observeAsState()
    AppThemeM3(darkTheme = viewState.isDark) {
        LaunchedEffect(key1 = viewEffect) {
            when (viewEffect?.consume()) {
                HtmlEpubReaderColorPickerViewEffect.NavigateBack -> onBack()
                null -> Unit
            }
        }
        CustomScaffoldM3(
            topBar = {
                HtmlEpubReaderColorPickerTopBar(
                    onDone = onBack,
                )
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight(0.7f).fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
            ) {
                ColorPicker(viewState, viewModel)
            }
        }
    }
}

@Composable
private fun ColorPicker(
    viewState: HtmlEpubReaderColorPickerViewState,
    viewModel: HtmlEpubReaderColorPickerViewModel
) {
    val selectedColor = viewState.selectedColor
    if (selectedColor != null) {
        FlowRow(
            modifier = Modifier.padding(horizontal = 10.dp),
        ) {
            viewState.colors.forEach { listColorHex ->
                FilterCircle(
                    hex = listColorHex,
                    isSelected = listColorHex == selectedColor,
                    onClick = { viewModel.onColorSelected(listColorHex) })
            }
        }
    }
}

@Composable
private fun FilterCircle(hex: String, isSelected: Boolean, onClick: () -> Unit) {
    val color = hex.toColorInt()
    Canvas(modifier = Modifier
        .padding(4.dp)
        .size(32.dp)
        .debounceClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        ), onDraw = {
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



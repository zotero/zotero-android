package org.zotero.android.pdf.colorpicker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.debounceClickable
import org.zotero.android.uicomponents.theme.CustomPalette
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.theme.CustomThemeWithStatusAndNavBars

@Composable
internal fun PdfReaderColorPickerScreen(
    onBack: () -> Unit,
    viewModel: PdfReaderColorPickerViewModel = hiltViewModel(),
) {
    viewModel.init()
    viewModel.setOsTheme(isDark = isSystemInDarkTheme())
    val viewState by viewModel.viewStates.observeAsState(PdfReaderColorPickerViewState())
    val viewEffect by viewModel.viewEffects.observeAsState()
    CustomThemeWithStatusAndNavBars(isDarkTheme = viewState.isDark) {
        LaunchedEffect(key1 = viewEffect) {
            when (viewEffect?.consume()) {
                PdfReaderColorPickerViewEffect.NavigateBack -> onBack()
                null -> Unit
            }
        }
        CustomScaffold(
            topBar = {
                PdfReaderColorPickerTopBar(
                    onDone = onBack,
                )
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight(0.7f)
                    .background(color = CustomTheme.colors.surface),
                verticalArrangement = Arrangement.Center,
            ) {
                ColorPicker(viewState, viewModel)
                val size = viewState.size
                if (size != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val layoutType = CustomLayoutSize.calculateLayoutType()
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            text = stringResource(id = Strings.size),
                            color = CustomTheme.colors.pdfSizePickerColor,
                            style = CustomTheme.typography.default,
                            fontSize = layoutType.calculatePdfSidebarTextSize(),
                        )
                        Slider(
                            modifier = Modifier.weight(1f),
                            value = size,
                            onValueChange = { viewModel.onSizeChanged(it) },
                            colors = SliderDefaults.colors(
                                activeTrackColor = CustomTheme.colors.zoteroDefaultBlue,
                                thumbColor = CustomTheme.colors.zoteroDefaultBlue
                            ),
                            valueRange = 0.5f..25f
                        )
                        Text(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            text = String.format("%.1f", size),
                            color = CustomTheme.colors.pdfSizePickerColor,
                            style = CustomTheme.typography.default,
                            fontSize = layoutType.calculatePdfSidebarTextSize(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorPicker(
    viewState: PdfReaderColorPickerViewState,
    viewModel: PdfReaderColorPickerViewModel
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
    val color = android.graphics.Color.parseColor(hex)
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



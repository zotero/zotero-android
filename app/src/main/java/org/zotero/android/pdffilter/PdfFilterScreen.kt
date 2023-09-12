package org.zotero.android.pdffilter

import android.graphics.Color.parseColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.theme.CustomPalette
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.theme.CustomThemeWithStatusAndNavBars
import org.zotero.android.uicomponents.topbar.HeadingTextButton

@Composable
internal fun PdfFilterScreen(
    onBack: () -> Unit,
    navigateToTagPicker: () -> Unit,
    viewModel: PdfFilterViewModel = hiltViewModel(),
) {
    viewModel.init()

    val layoutType = CustomLayoutSize.calculateLayoutType()
    val viewState by viewModel.viewStates.observeAsState(PdfFilterViewState())
    val viewEffect by viewModel.viewEffects.observeAsState()
    CustomThemeWithStatusAndNavBars(isDarkTheme = viewState.isDark) {
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
        CustomScaffold(
            topBar = {
                TopBar(
                    onClose = viewModel::onClose,
                    onClear = if (viewState.isClearVisible()) viewModel::onClear else null
                )
            },
        ) {
            Column(
                modifier = Modifier
                    .background(color = CustomTheme.colors.surface)
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
                TagsListAndSelect(
                    viewState = viewState,
                    viewModel = viewModel,
                    layoutType = layoutType
                )
            }
        }
    }
}

@Composable
private fun TagsListAndSelect(
    viewState: PdfFilterViewState,
    viewModel: PdfFilterViewModel,
    layoutType: CustomLayoutSize.LayoutType
) {
    if (!viewState.availableTags.isEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 20.dp)
                .safeClickable(
                    onClick = viewModel::onTagsClicked,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val formattedTags = viewState.formattedTags()
            Text(
                modifier = Modifier.weight(1f),
                text = if (formattedTags.isEmpty()) stringResource(id = Strings.pdf_annotations_sidebar_filter_tags_placeholder) else formattedTags,
                color = CustomTheme.colors.primaryContent,
                style = CustomTheme.typography.default,
                fontSize = layoutType.calculateTextSize(),
            )

            Icon(
                painter = painterResource(Drawables.ic_arrow_small_right),
                contentDescription = null,
                modifier = Modifier.padding(start = 8.dp),
                tint = CustomTheme.colors.secondaryContent,
            )
        }

    }
}

@Composable
fun FilterCircle(hex: String, isSelected: Boolean, onClick: () -> Unit) {
    val color = parseColor(hex)
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

@Composable
private fun TopBar(
    onClose: () -> Unit,
    onClear: (() -> Unit)?,
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = stringResource(id = Strings.pdf_annotations_sidebar_filter_title),
                color = CustomTheme.colors.primaryContent,
                style = CustomTheme.typography.h2
            )
        },
        navigationIcon = {
            Row {
                Spacer(modifier = Modifier.width(8.dp))
                HeadingTextButton(
                    onClick = onClose,
                    text = stringResource(Strings.close),
                )
            }
        },
        actions = {
            if (onClear != null) {
                HeadingTextButton(
                    onClick = onClear,
                    text = stringResource(Strings.clear),
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = CustomTheme.colors.surface),
    )
}



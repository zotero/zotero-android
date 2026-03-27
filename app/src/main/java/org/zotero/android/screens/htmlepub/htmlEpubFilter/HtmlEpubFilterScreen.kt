package org.zotero.android.screens.htmlepub.htmlEpubFilter

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.core.graphics.toColorInt
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.uicomponents.CustomScaffoldM3
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.foundation.safeClickable
import org.zotero.android.uicomponents.theme.CustomPalette
import org.zotero.android.uicomponents.themem3.AppThemeM3

@Composable
internal fun HtmlEpubFilterScreen(
    onBack: () -> Unit,
    navigateToTagPicker: () -> Unit,
    viewModel: HtmlEpubFilterViewModel = hiltViewModel(),
) {
    viewModel.init()

    val viewState by viewModel.viewStates.observeAsState(HtmlEpubFilterViewState())
    val viewEffect by viewModel.viewEffects.observeAsState()
    AppThemeM3(darkTheme = viewState.isDark) {
        LaunchedEffect(key1 = viewEffect) {
            when (viewEffect?.consume()) {
                null -> Unit
                is HtmlEpubFilterViewEffect.OnBack -> {
                    onBack()
                }

                is HtmlEpubFilterViewEffect.NavigateToTagPickerScreen -> {
                    navigateToTagPicker()
                }
            }
        }
        CustomScaffoldM3(
            topBar = {
                HtmlEpubFilterScreenTopBar(
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
                HtmlEpubFilterTagsListAndSelect(
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


@Composable
private fun HtmlEpubFilterTagsListAndSelect(
    viewState: HtmlEpubFilterViewState,
    viewModel: HtmlEpubFilterViewModel,
) {
    if (!viewState.availableTags.isEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .safeClickable(
                    onClick = viewModel::onTagsClicked,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val formattedTags = viewState.formattedTags()
            Text(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                text = formattedTags.ifEmpty {
                    stringResource(id = Strings.pdf_annotations_sidebar_filter_tags_placeholder)
                },
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}


@Composable
private fun HtmlEpubFilterScreenTopBar(
    onBack: () -> Unit,
    onClear: (() -> Unit)?,
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        title = {
            Text(
                text = stringResource(Strings.pdf_annotations_sidebar_filter_title),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(Drawables.arrow_back_24dp),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        actions = {
            if (onClear != null) {
                FilledTonalButton(
                    onClick = onClear,
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    Text(
                        text = stringResource(Strings.clear),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
        },
    )
}

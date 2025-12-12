package org.zotero.android.pdf.reader.topbar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults.rememberTooltipPositionProvider
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.pdf.reader.PdfReaderVMInterface
import org.zotero.android.pdf.reader.PdfReaderViewState
import org.zotero.android.pdf.reader.pdfsearch.PdfReaderSearchViewModel
import org.zotero.android.pdf.reader.pdfsearch.PdfReaderSearchViewState
import org.zotero.android.pdf.reader.pdfsearch.popup.PdfReaderSearchPopup
import org.zotero.android.pdf.reader.share.PdfReaderSharePopup
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.icon.IconWithPaddingM3

@Composable
internal fun PdfReaderTopBar(
    onBack: () -> Unit,
    onShowHideSideBar: () -> Unit,
    onShareButtonTapped: () -> Unit,
    toPdfSettings: () -> Unit,
    toPdfPlainReader: () -> Unit,
    onShowHidePdfSearch: () -> Unit,
    toggleToolbarButton: () -> Unit,
    isToolbarButtonSelected: Boolean,
    showSideBar: Boolean,
    showPdfSearch: Boolean,
    viewState: PdfReaderViewState,
    viewModel: PdfReaderVMInterface,
    pdfReaderSearchViewModel: PdfReaderSearchViewModel,
    pdfReaderSearchViewState: PdfReaderSearchViewState,
) {
    val isTablet = CustomLayoutSize.calculateLayoutType().isTablet()

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        title = {},
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
            Spacer(modifier = Modifier.width(48.dp))
            TooltipBox(
                positionProvider = rememberTooltipPositionProvider(
                    TooltipAnchorPosition.Below,
                    4.dp
                ),
                tooltip = {
                    PlainTooltip() {
                        Text(
                            stringResource(
                                Strings.pdf_reader_sidebar
                            )
                        )
                    }
                },
                state = rememberTooltipState()
            ) {
                IconWithPaddingM3(
                    unselectedDrawableRes = Drawables.view_sidebar,
                    selectedDrawableRes = Drawables.view_sidebar_filled,
                    onToggle = {
                        onShowHideSideBar()
                    },
                    isSelected = showSideBar
                )
            }

            TooltipBox(
                positionProvider = rememberTooltipPositionProvider(
                    TooltipAnchorPosition.Below,
                    4.dp
                ),
                tooltip = {
                    PlainTooltip() {
                        Text(
                            stringResource(
                                Strings.pdf_reader_plain_reader
                            )
                        )
                    }
                },
                state = rememberTooltipState()
            ) {
                IconButton(onClick = toPdfPlainReader) {
                    Icon(
                        painter = painterResource(Drawables.reader),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }

            }

            Spacer(modifier = Modifier.weight(1f))
            TooltipBox(
                positionProvider = rememberTooltipPositionProvider(
                    TooltipAnchorPosition.Below,
                    4.dp
                ),
                tooltip = {
                    PlainTooltip() {
                        Text(
                            stringResource(
                                Strings.pdf_reader_toolbar
                            )
                        )
                    }
                },
                state = rememberTooltipState()
            ) {
                IconWithPaddingM3(
                    unselectedDrawableRes = Drawables.draw,
                    selectedDrawableRes = Drawables.draw_filled,
                    onToggle = toggleToolbarButton,
                    isSelected = isToolbarButtonSelected
                )
            }

            if (isTablet) {
                Box {
                    TooltipBox(
                        positionProvider = rememberTooltipPositionProvider(
                            TooltipAnchorPosition.Below,
                            4.dp
                        ),
                        tooltip = {
                            PlainTooltip() {
                                Text(
                                    stringResource(
                                        Strings.searchbar_placeholder
                                    )
                                )
                            }
                        },
                        state = rememberTooltipState()
                    ) {
                        IconWithPaddingM3(
                            unselectedDrawableRes = Drawables.search,
                            selectedDrawableRes = Drawables.search,
                            onToggle = onShowHidePdfSearch,
                            isSelected = true,
                        )

                    }
                    if (viewState.showPdfSearch) {
                        PdfReaderSearchPopup(
                            viewModel = viewModel,
                            pdfReaderSearchViewState = pdfReaderSearchViewState,
                            pdfReaderSearchViewModel = pdfReaderSearchViewModel,
                        )
                    }
                }
            } else {
                TooltipBox(
                    positionProvider = rememberTooltipPositionProvider(
                        TooltipAnchorPosition.Below,
                        4.dp
                    ),
                    tooltip = {
                        PlainTooltip() {
                            Text(
                                stringResource(
                                    Strings.searchbar_placeholder
                                )
                            )
                        }
                    },
                    state = rememberTooltipState()
                ) {
                    IconWithPaddingM3(
                        unselectedDrawableRes = Drawables.search,
                        selectedDrawableRes = Drawables.search,
                        onToggle = {
                            onShowHidePdfSearch()
                        },
                        isSelected = showPdfSearch
                    )
                }
            }
            Box {
                if (viewState.showSharePopup) {
                    PdfReaderSharePopup(
                        viewModel = viewModel,
                        viewState = viewState,
                    )
                }
                TooltipBox(
                    positionProvider = rememberTooltipPositionProvider(
                        TooltipAnchorPosition.Below,
                        4.dp
                    ),
                    tooltip = {
                        PlainTooltip() {
                            Text(
                                stringResource(
                                    Strings.share
                                )
                            )
                        }
                    },
                    state = rememberTooltipState()
                ) {
                    IconWithPaddingM3(
                        unselectedDrawableRes = Drawables.share_24,
                        selectedDrawableRes = Drawables.share_24,
                        onToggle = onShareButtonTapped,
                        isSelected = false
                    )

                }
            }

            TooltipBox(
                positionProvider = rememberTooltipPositionProvider(
                    TooltipAnchorPosition.Below,
                    4.dp
                ),
                tooltip = {
                    PlainTooltip() {
                        Text(
                            stringResource(
                                Strings.settings_title
                            )
                        )
                    }
                },
                state = rememberTooltipState()
            ) {
                IconWithPaddingM3(
                    unselectedDrawableRes = Drawables.match_case_24,
                    selectedDrawableRes = Drawables.match_case_24,
                    onToggle = toPdfSettings,
                    isSelected = false
                )

            }
        },
    )
}

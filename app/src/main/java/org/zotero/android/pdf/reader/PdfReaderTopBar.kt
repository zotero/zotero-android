package org.zotero.android.pdf.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.architecture.ui.CustomLayoutSize
import org.zotero.android.pdf.reader.pdfsearch.PdfReaderSearchPopup
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.icon.IconWithPadding
import org.zotero.android.uicomponents.icon.ToggleIconWithPadding
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.topbar.NewCustomTopBar
import org.zotero.android.uicomponents.topbar.NewHeadingTextButton

@Composable
internal fun PdfReaderTopBar(
    onBack: () -> Unit,
    onShowHideSideBar: () -> Unit,
    toPdfSettings: () -> Unit,
    toPdfPlainReader: () -> Unit,
    onShowHidePdfSearch: () -> Unit,
    toggleToolbarButton: () -> Unit,
    isToolbarButtonSelected: Boolean,
    showSideBar: Boolean,
    showPdfSearch: Boolean,
    viewState: PdfReaderViewState,
    viewModel: PdfReaderVMInterface
) {
    val isTablet = CustomLayoutSize.calculateLayoutType().isTablet()
    NewCustomTopBar(
        backgroundColor = CustomTheme.colors.surface,
        leftContainerContent = listOf(
            {
                NewHeadingTextButton(
                    onClick = onBack,
                    text = stringResource(Strings.back_button),
                )
            },
            {
                ToggleIconWithPadding(
                    drawableRes = Drawables.view_sidebar_24px,
                    onToggle = {
                        onShowHideSideBar()
                    },
                    isSelected = showSideBar

                )
            },
            {
                IconWithPadding(drawableRes = Drawables.pdf_raw_reader, onClick = toPdfPlainReader)
            },
        ), rightContainerContent = listOf(
            {
                ToggleIconWithPadding(
                    drawableRes = Drawables.draw_24px,
                    onToggle = toggleToolbarButton,
                    isSelected = isToolbarButtonSelected
                )
            },
            {
                if (isTablet) {
                    Box {
                        IconWithPadding(
                            drawableRes = Drawables.search_24px,
                            onClick = onShowHidePdfSearch
                        )
                        if (viewState.showPdfSearch) {
                            PdfReaderSearchPopup(
                                viewState = viewState,
                                viewModel = viewModel,
                            )
                        }
                    }
                } else {
                    ToggleIconWithPadding(
                        drawableRes = Drawables.search_24px,
                        onToggle = {
                            onShowHidePdfSearch()
                        },
                        isSelected = showPdfSearch
                    )
                }
            },
            {
                IconWithPadding(drawableRes = Drawables.settings_24px, onClick = toPdfSettings)
            },
        )
    )
}

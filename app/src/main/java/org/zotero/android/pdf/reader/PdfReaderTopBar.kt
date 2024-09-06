package org.zotero.android.pdf.reader

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
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
    toggleToolbarButton:() -> Unit,
    isToolbarButtonSelected: Boolean,
    showSideBar: Boolean,
) {
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
            }
        ), rightContainerContent = listOf(
            {
                ToggleIconWithPadding(
                    drawableRes = Drawables.draw_24px,
                    onToggle = toggleToolbarButton,
                    isSelected = isToolbarButtonSelected
                )
            },
            {
                IconWithPadding(drawableRes = Drawables.settings_24px, onClick = toPdfSettings)
            }
        )
    )
}

package org.zotero.android.pdf.reader

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.icon.IconWithPadding
import org.zotero.android.uicomponents.icon.ToggleIconWithPadding
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.topbar.HeadingTextButton

@Composable
internal fun PdfReaderTopBar(
    onBack: () -> Unit,
    onShowHideSideBar: () -> Unit,
    toPdfSettings: () -> Unit,
    toggleToolbarButton:() -> Unit,
    isToolbarButtonSelected: Boolean,
    showSideBar: Boolean,
) {
    CenterAlignedTopAppBar(
        navigationIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.width(4.dp))
                HeadingTextButton(
                    onClick = onBack,
                    text = stringResource(Strings.back),
                )
                Spacer(modifier = Modifier.width(8.dp))
                ToggleIconWithPadding(
                    drawableRes = Drawables.view_sidebar_24px,
                    onToggle = {
                        onShowHideSideBar()
                    },
                    isSelected = showSideBar

                )
            }
        },
        actions = {
            ToggleIconWithPadding(
                drawableRes = Drawables.draw_24px,
                onToggle = toggleToolbarButton,
                isSelected = isToolbarButtonSelected
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconWithPadding(drawableRes = Drawables.settings_24px, onClick = toPdfSettings)
        },
        title = {

        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = CustomTheme.colors.surface),
    )

}

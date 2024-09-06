package org.zotero.android.screens.scanbarcode.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.topbar.NewCustomTopBar
import org.zotero.android.uicomponents.topbar.NewHeadingTextButton

@Composable
internal fun ScanBarcodeCloseScanAnotherTopBar(
    onClose: () -> Unit,
    onScan: () -> Unit,
) {
    NewCustomTopBar(
        leftContainerContent = listOf {
            NewHeadingTextButton(
                text = stringResource(id = Strings.close),
                onClick = onClose
            )
        },
        rightContainerContent = listOf {
            NewHeadingTextButton(
                style = CustomTheme.typography.defaultBold,
                text = stringResource(id = Strings.scan_barcode_scan_another_button),
                onClick = onScan
            )
        }
    )
}
package org.zotero.android.screens.scanbarcode.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.zotero.android.screens.scanbarcode.ScanBarcodeViewEffect.NavigateBack
import org.zotero.android.screens.scanbarcode.ScanBarcodeViewModel
import org.zotero.android.screens.scanbarcode.ScanBarcodeViewModel.Error
import org.zotero.android.screens.scanbarcode.ScanBarcodeViewModel.State
import org.zotero.android.screens.scanbarcode.ScanBarcodeViewState
import org.zotero.android.uicomponents.CustomScaffold
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.theme.CustomPalette
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.theme.CustomThemeWithStatusAndNavBars

@Composable
internal fun ScanBarcodeScreen(
    viewModel: ScanBarcodeViewModel = hiltViewModel(),
    onClose: () -> Unit,
) {
    CustomThemeWithStatusAndNavBars(
        statusBarBackgroundColor = CustomTheme.colors.topBarBackgroundColor,
        navBarBackgroundColor = CustomTheme.colors.zoteroItemDetailSectionBackground
    ) {
        val viewState by viewModel.viewStates.observeAsState(ScanBarcodeViewState())
        val viewEffect by viewModel.viewEffects.observeAsState()
        LaunchedEffect(key1 = viewModel) {
            viewModel.init()
        }
        LaunchedEffect(key1 = viewEffect) {
            when (viewEffect?.consume()) {
                is NavigateBack -> {
                    onClose()
                }

                else -> {}
            }
        }
        CustomScaffold(
            topBar = {
                ScanBarcodeCloseScanAnotherTopBar(
                    onClose = onClose,
                    onScan = viewModel::launchBarcodeScanner
                )
            },
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = CustomTheme.colors.zoteroItemDetailSectionBackground)
                    .padding(horizontal = 16.dp)
            ) {
                if (viewState.lookupState == State.waitingInput) {
                    scanBarcodeLoadingIndicator()
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = stringResource(id = Strings.scan_barcode_loading_translators),
                                style = CustomTheme.typography.newBody,
                                color = CustomTheme.colors.primaryContent,
                            )
                        }
                    }
                } else {
                    scanBarcodeTable(
                        rows = viewState.lookupRows,
                        onDelete = { viewModel.onItemDelete(it) })
                    if (viewState.lookupState == State.loadingIdentifiers) {
                        scanBarcodeLoadingIndicator()
                    }
                    scanBarcodeError(
                        failedState = viewState.lookupState as? State.failed
                    )
                }
            }
        }
    }
}

internal fun LazyListScope.scanBarcodeError(
    failedState: State.failed?
) {
    if (failedState != null) {
        item {
            val errorText = when (failedState.error) {
                is Error.noIdentifiersDetectedAndNoLookupData -> {
                    stringResource(id = Strings.errors_lookup)
                }

                is Error.noIdentifiersDetectedWithLookupData -> {
                    stringResource(id = Strings.scar_barcode_error_lookup_no_new_identifiers_found)
                }

                else -> {
                    stringResource(id = Strings.errors_unknown)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = errorText,
                style = CustomTheme.typography.newBody,
                color = CustomPalette.ErrorRed,
            )
        }

    }
}

internal fun LazyListScope.scanBarcodeLoadingIndicator() {
    item {
        Spacer(modifier = Modifier.height(30.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            CircularProgressIndicator(
                color = CustomTheme.colors.zoteroDefaultBlue,
                modifier = Modifier
                    .size(48.dp)
            )
        }
    }
}
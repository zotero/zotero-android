package org.zotero.android.screens.scanbarcode.ui

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import org.zotero.android.screens.scanbarcode.ScanBarcodeViewModel.State
import org.zotero.android.screens.scanbarcode.ScanBarcodeViewState
import org.zotero.android.screens.scanbarcode.ui.rows.ScanBarcodeScanAnotherRow
import org.zotero.android.uicomponents.CustomScaffoldM3
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.theme.CustomTheme
import org.zotero.android.uicomponents.themem3.AppThemeM3

@Composable
internal fun ScanBarcodeScreen(
    viewModel: ScanBarcodeViewModel = hiltViewModel(),
    onClose: () -> Unit,
) {
    AppThemeM3 {
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
        CustomScaffoldM3(
            topBar = {
                ScanBarcodeTopBar(
                    onDone = onClose,
                )
            },
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 8.dp)
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
//                        scanBarcodeLoadingIndicator()
                    } else {
                        item {
                            ScanBarcodeScanAnotherRow(onClick = viewModel::launchBarcodeScanner)
                        }
                    }
                }
            }
        }
    }
}

internal fun LazyListScope.scanBarcodeLoadingIndicator() {
    item {
        Spacer(modifier = Modifier.height(30.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(48.dp)
            )
        }
    }
}
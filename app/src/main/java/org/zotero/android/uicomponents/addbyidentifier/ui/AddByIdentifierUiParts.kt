package org.zotero.android.uicomponents.addbyidentifier.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.addbyidentifier.AddByIdentifierViewModel
import org.zotero.android.uicomponents.addbyidentifier.AddByIdentifierViewState
import org.zotero.android.uicomponents.textinput.CustomTextField
import org.zotero.android.uicomponents.theme.CustomPalette
import org.zotero.android.uicomponents.theme.CustomTheme

internal fun LazyListScope.addByIdentifierTitleEditFieldAndError(
    viewState: AddByIdentifierViewState,
    viewModel: AddByIdentifierViewModel,
    failedState: AddByIdentifierViewModel.State.failed?
) {
    item {
        Spacer(modifier = Modifier.height(20.dp))
        IdentifierTitle()

        Spacer(modifier = Modifier.height(12.dp))
        IdentifierEditField(
            identifierText = viewState.identifierText,
            onIdentifierTextChange = viewModel::onIdentifierTextChange,
        )

//        Spacer(modifier = Modifier.height(20.dp))
//        ScanTextButton(onClick = viewModel::onScanText)

        if (failedState != null) {
            val errorText = when (failedState.error) {
                is AddByIdentifierViewModel.Error.noIdentifiersDetectedAndNoLookupData -> {
                    stringResource(id = Strings.errors_lookup)
                }

                is AddByIdentifierViewModel.Error.noIdentifiersDetectedWithLookupData -> {
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

@Composable
internal fun IdentifierTitle() {
    Text(
        text = stringResource(id = Strings.lookup_title),
        color = CustomPalette.DarkGrayColor,
        style = CustomTheme.typography.subhead,
    )
}

@Composable
internal fun IdentifierEditField(
    identifierText: String,
    onIdentifierTextChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .background(
                color = CustomTheme.colors.zoteroEditFieldBackground,
                shape = RoundedCornerShape(size = 10.dp)
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
        CustomTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            value = identifierText,
            hint = "",
            minLines = 4,
            maxLines = 4,
            ignoreTabsAndCaretReturns = false,
            focusRequester = focusRequester,
            textColor = CustomTheme.colors.primaryContent,
            onValueChange = onIdentifierTextChange,
            textStyle = CustomTheme.typography.newBody,
        )
    }
}

internal fun LazyListScope.addByIdentifierLoadingIndicator() {
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

//@Composable
//internal fun ScanTextButton(onClick: () -> Unit) {
//    Column(modifier = Modifier.fillMaxWidth()) {
//        Row(
//            verticalAlignment = Alignment.CenterVertically,
//            modifier = Modifier
//                .clickable(
//                    interactionSource = remember { MutableInteractionSource() },
//                    indication = null,
//                    onClick = onClick
//                )
//                .align(Alignment.CenterHorizontally)
//        ) {
//            Icon(
//                modifier = Modifier.size(24.dp),
//                painter = painterResource(id = Drawables.baseline_qr_code_scanner_24),
//                contentDescription = null,
//                tint = CustomTheme.colors.zoteroDefaultBlue,
//            )
//            Spacer(modifier = Modifier.width(12.dp))
//            Text(
//                text = stringResource(id = Strings.scan_text),
//                color = CustomTheme.colors.zoteroDefaultBlue,
//                style = CustomTheme.typography.newBody,
//            )
//        }
//
//    }
//}

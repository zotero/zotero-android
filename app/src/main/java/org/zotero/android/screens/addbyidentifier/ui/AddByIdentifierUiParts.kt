package org.zotero.android.screens.addbyidentifier.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.zotero.android.screens.addbyidentifier.AddByIdentifierViewModel
import org.zotero.android.screens.addbyidentifier.AddByIdentifierViewState
import org.zotero.android.uicomponents.Strings
import org.zotero.android.uicomponents.textinput.CustomTextField
import org.zotero.android.uicomponents.theme.CustomPalette
import org.zotero.android.uicomponents.theme.CustomTheme

internal fun LazyListScope.addByIdentifierTitleEditFieldAndError(
    viewState: AddByIdentifierViewState,
    viewModel: AddByIdentifierViewModel,
    failedState: AddByIdentifierViewModel.State.failed?
) {
    item {
        Spacer(modifier = Modifier.height(4.dp))
        IdentifierTitle()
        Spacer(modifier = Modifier.height(16.dp))
        IdentifierEditField(
            identifierText = viewState.identifierText,
            onIdentifierTextChange = viewModel::onIdentifierTextChange,
        )

        if (failedState != null) {
            val errorText = when (failedState.error) {
                is AddByIdentifierViewModel.Error.noIdentifiersDetectedAndNoLookupData -> {
                    stringResource(id = Strings.errors_lookup_no_identifiers_and_no_lookup_data)
                }

                is AddByIdentifierViewModel.Error.noIdentifiersDetectedWithLookupData -> {
                    stringResource(id = Strings.errors_lookup_no_identifiers_with_lookup_data)
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
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
internal fun IdentifierEditField(
    identifierText: String,
    onIdentifierTextChange: (String) -> Unit,
) {
    val roundedCornerShape = RoundedCornerShape(size = 6.dp)
    Row(
        modifier = Modifier
            .background(
                color = Color.Transparent,
                shape = roundedCornerShape
            )
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = roundedCornerShape
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
            textColor = MaterialTheme.colorScheme.onSurface,
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

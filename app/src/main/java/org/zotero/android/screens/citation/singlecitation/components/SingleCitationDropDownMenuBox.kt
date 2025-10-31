package org.zotero.android.screens.citation.singlecitation.components

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.zotero.android.screens.citation.singlecitation.SingleCitationViewModel
import org.zotero.android.screens.citation.singlecitation.SingleCitationViewState
import org.zotero.android.screens.citation.singlecitation.locatorsList

@Composable
internal fun SingleCitationDropDownMenuBox(
    viewState: SingleCitationViewState,
    viewModel: SingleCitationViewModel
) {

    var expanded by remember {
        mutableStateOf(false)
    }
    ExposedDropdownMenuBox(
        modifier = Modifier
            .width(142.dp)
            .clip(RoundedCornerShape(10.dp))
            .requiredSizeIn(maxHeight = 52.dp),
        expanded = expanded,
        onExpandedChange = {
            expanded = !expanded
        }
    ) {
        TextField(
            modifier = Modifier.menuAnchor(),
            value = localized(viewState.locator),
            onValueChange = { },
            textStyle = MaterialTheme.typography.bodyLarge,
            colors = TextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.primary,
                unfocusedTextColor = MaterialTheme.colorScheme.primary,
                disabledTextColor = MaterialTheme.colorScheme.primary,
                errorTextColor = MaterialTheme.colorScheme.primary,

                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                disabledContainerColor = MaterialTheme.colorScheme.surface,
                errorContainerColor = MaterialTheme.colorScheme.surface,

                focusedTrailingIconColor = MaterialTheme.colorScheme.primary,
                unfocusedTrailingIconColor = MaterialTheme.colorScheme.primary,
                disabledTrailingIconColor = MaterialTheme.colorScheme.primary,
                errorTrailingIconColor = MaterialTheme.colorScheme.primary,

                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,

                ),
            readOnly = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
        )

        ExposedDropdownMenu(
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer),
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {
            locatorsList.forEachIndexed { index, item ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = localized(item),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    },
                    onClick = {
                        viewModel.setLocator(item)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@SuppressLint("DiscouragedApi")
@Composable
private fun localized(locator: String): String {
    val context = LocalContext.current
    val resourceId =
        context.resources.getIdentifier(
            "citation.locator.${locator.replace(' ', '_')}",
            "string",
            context.packageName
        )
    val stringResource = stringResource(resourceId)
    return stringResource
}
package org.zotero.android.pdf.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.zotero.android.pdf.settings.data.MultiSelectorOption
import org.zotero.android.pdf.settings.data.PdfSettingsOptions
import org.zotero.android.uicomponents.Drawables

internal fun LazyListScope.pdfSettingsSettingRow(
    @StringRes titleResId: Int,
    options: List<PdfSettingsOptions>,
    selectedOption: PdfSettingsOptions,
    optionSelected: (Int) -> Unit,
) {
    item {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(id = titleResId),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))

            val optionsList = options.map { opt ->
                MultiSelectorOption(
                    ordinal = opt.ordinal,
                    optionString = stringResource(id = opt.optionStringId)
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            ) {
                optionsList.forEachIndexed { index, opt ->
                    val isSelected = opt.ordinal == selectedOption.ordinal
                    ToggleButton(
                        checked = isSelected,
                        onCheckedChange = { optionSelected(opt.ordinal) },
                        modifier = Modifier
                            .weight(1f)
                            .semantics { role = Role.RadioButton },
                        shapes = when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                        colors = ToggleButtonDefaults.toggleButtonColors(
                            checkedContainerColor = MaterialTheme.colorScheme.secondary,
                            checkedContentColor = MaterialTheme.colorScheme.onSecondary,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                    ) {
                        if (isSelected) {
                            Icon(
                                painter = painterResource(id = Drawables.check_24px),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondary
                            )
                            Spacer(Modifier.size(6.dp))
                        }
                        Text(
                            text = opt.optionString,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

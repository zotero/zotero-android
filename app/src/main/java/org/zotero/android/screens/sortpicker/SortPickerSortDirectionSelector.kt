package org.zotero.android.screens.sortpicker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.zotero.android.uicomponents.Drawables
import org.zotero.android.uicomponents.Strings

@Composable
internal fun SortPickerSortDirectionSelector(
    viewState: SortPickerViewState,
    viewModel: SortPickerViewModel
) {
    val ascendingOptionLabel = stringResource(id = Strings.items_ascending)
    val descendingOptionLabel = stringResource(id = Strings.items_descending)

    val optionsList = listOf(ascendingOptionLabel, descendingOptionLabel)

    val selectedOptionIndex = if (viewState.isAscending) {
        optionsList.indexOf(ascendingOptionLabel)
    } else {
        optionsList.indexOf(descendingOptionLabel)
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        optionsList.forEachIndexed { index, opt ->
            val isSelected = index == selectedOptionIndex
            ToggleButton(
                checked = isSelected,
                onCheckedChange = { viewModel.onSortDirectionChanged(isAscending = opt == ascendingOptionLabel) },
                modifier = Modifier
                    .weight(1f)
                    .semantics { role = Role.RadioButton },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    optionsList.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
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
                    text = opt,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1
                )
            }
        }
    }
}
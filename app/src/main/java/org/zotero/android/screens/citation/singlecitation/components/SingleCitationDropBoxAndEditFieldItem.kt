package org.zotero.android.screens.citation.singlecitation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.zotero.android.screens.citation.singlecitation.SingleCitationViewModel
import org.zotero.android.screens.citation.singlecitation.SingleCitationViewState
import org.zotero.android.uicomponents.textinput.CustomTextField

@Composable
internal fun SingleCitationDropBoxAndEditFieldItem(
    viewState: SingleCitationViewState,
    viewModel: SingleCitationViewModel
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SingleCitationDropDownMenuBox(viewState, viewModel)

        CustomTextField(
            modifier = Modifier
                .padding(horizontal = 16.dp),
            value = viewState.locatorValue,
            hint = "Number",
            ignoreTabsAndCaretReturns = true,
            maxLines = 1,
            singleLine = true,
            onValueChange = viewModel::onLocatorValueChanged,
            textStyle = MaterialTheme.typography.bodyLarge,
            textColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

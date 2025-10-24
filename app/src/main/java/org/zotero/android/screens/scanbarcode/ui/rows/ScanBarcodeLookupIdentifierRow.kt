package org.zotero.android.screens.scanbarcode.ui.rows

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.zotero.android.screens.addbyidentifier.data.LookupRow
import org.zotero.android.uicomponents.Strings

@Composable
internal fun ScanBarcodeLookupIdentifierRow(
    title: String,
    state: LookupRow.IdentifierState,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(48.dp)
            .padding(end = 16.dp)
    ) {
        Spacer(modifier = Modifier.width(44.dp))
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp),
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        when (state) {
            LookupRow.IdentifierState.enqueued -> {
                Text(
                    text = stringResource(id = Strings.scan_barcode_item_queued_state),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            LookupRow.IdentifierState.failed -> {
                Text(
                    text = stringResource(id = Strings.scan_barcode_item_failed_state),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            LookupRow.IdentifierState.inProgress -> {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(24.dp)
                )
            }
        }
    }


}
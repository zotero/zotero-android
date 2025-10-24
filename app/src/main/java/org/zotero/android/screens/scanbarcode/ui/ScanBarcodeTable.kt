package org.zotero.android.screens.scanbarcode.ui

import androidx.compose.foundation.lazy.LazyListScope
import org.zotero.android.screens.addbyidentifier.data.LookupRow
import org.zotero.android.screens.scanbarcode.ui.rows.ScanBarcodeLookupAttachmentRow
import org.zotero.android.screens.scanbarcode.ui.rows.ScanBarcodeLookupIdentifierRow
import org.zotero.android.screens.scanbarcode.ui.rows.ScanBarcodeLookupItemRow

internal fun LazyListScope.scanBarcodeTable(
    rows: List<LookupRow>,
    onDelete: (row: LookupRow.item) -> Unit
) {
    rows.forEach { row ->
        item {
            when (row) {
                is LookupRow.item -> {
                    val data = row.item
                    ScanBarcodeLookupItemRow(
                        title = data.title,
                        type = data.type,
                        onDelete = { onDelete(row) }
                    )
                }

                is LookupRow.attachment -> {
                    val attachment = row.attachment
                    val update = row.updateKind
                    ScanBarcodeLookupAttachmentRow(
                        title = attachment.title,
                        attachmentType = attachment.type,
                        update = update
                    )
                }

                is LookupRow.identifier -> {
                    val identifier = row.identifier
                    val state = row.state
                    ScanBarcodeLookupIdentifierRow(
                        title = identifier,
                        state = state,
                    )
                }
            }
        }
    }
}
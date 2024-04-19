package org.zotero.android.screens.share.data

import org.zotero.android.helpers.formatter.sqlFormat
import java.util.Date

data class TranslatorMetadata(
    val id: String,
    val lastUpdated: String,
    val fileName: String
) {
    val lastUpdatedDate: Date
        get() {
            return sqlFormat.parse(lastUpdated)!!
        }
}

package org.zotero.android.screens.share.data

import org.zotero.android.helpers.formatter.sqlFormat
import timber.log.Timber
import java.util.Date

data class TranslatorMetadata(
    val id: String,
    val lastUpdated: Date,
    val fileName: String
) {
    sealed class Error : Exception() {
        object incorrectDateFormat : Error()
    }

    companion object {
        fun from(id: String, filename: String, rawLastUpdated: String): TranslatorMetadata {
            val lastUpdated: Date
            try {
                lastUpdated = sqlFormat.parse(rawLastUpdated)!!
            } catch (e: Exception) {
                Timber.e(
                    e,
                    "TranslatorMetadata: translator $id has incorrect date format - \"$rawLastUpdated\""
                )
                throw Error.incorrectDateFormat
            }
            return TranslatorMetadata(
                id = id,
                lastUpdated = lastUpdated,
                fileName = filename
            )
        }
    }
}

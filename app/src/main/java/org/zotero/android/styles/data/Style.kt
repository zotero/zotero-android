package org.zotero.android.styles.data

import org.zotero.android.database.objects.RStyle
import java.util.Date

data class Style(
    val identifier: String,
    val title: String,
    val updated: Date,
    val href: String,
    val filename: String,
    val supportsBibliography: Boolean,
    val isNoteStyle: Boolean,
    val dependencyId: String?,
    val defaultLocale: String?,
) {
    val id: String get() {
        return this.identifier
    }
}
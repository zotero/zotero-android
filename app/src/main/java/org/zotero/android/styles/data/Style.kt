package org.zotero.android.styles.data

import org.zotero.android.database.objects.RStyle
import timber.log.Timber
import java.net.URL
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
    val id: String
        get() {
            return this.identifier
        }

    companion object {
        fun fromRStyle(rStyle: RStyle): Style? {
            val href: String
            try {
                URL(rStyle.href)
                href = rStyle.href
            } catch (e: Exception) {
                Timber.e(e, "Style: RStyle has wrong href - \"${rStyle.href}\"")
                return null
            }

            return Style(
                identifier = rStyle.identifier,
                title = rStyle.title,
                updated = rStyle.updated,
                href = href,
                filename = rStyle.filename,
                supportsBibliography = rStyle.supportsBibliography,
                isNoteStyle = rStyle.isNoteStyle,
                dependencyId = rStyle.dependency?.identifier,
                defaultLocale = rStyle.defaultLocale.ifEmpty { null }
            )
        }
    }
}
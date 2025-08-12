package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import org.zotero.android.database.DbResponseRequest
import org.zotero.android.database.objects.RStyle
import org.zotero.android.styles.data.Style

class SyncStylesDbRequest(
    private val styles: List<Style>
) : DbResponseRequest<List<String>> {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm): List<String> {
        val update = mutableListOf<String>()

        for (style in this.styles) {
            val rStyle: RStyle
            val existing =
                database.where<RStyle>().equalTo("identifier", style.identifier).findFirst()
            if (existing != null) {
                if (existing.updated.time - style.updated.time >= 0) {
                    continue
                }
                rStyle = existing
            } else {
                rStyle = database.createObject<RStyle>(style.identifier)
                rStyle.installed = true
            }

            rStyle.href = style.href
            rStyle.title = style.title
            rStyle.updated = style.updated
            rStyle.filename = style.filename
            rStyle.supportsBibliography = style.supportsBibliography
            rStyle.isNoteStyle = style.isNoteStyle
            rStyle.defaultLocale = style.defaultLocale ?: ""
            update.add(style.filename)
        }

        return update
    }
}
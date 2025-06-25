package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import org.zotero.android.database.DbRequest
import org.zotero.android.database.objects.RStyle
import org.zotero.android.styles.data.Style

class StoreStyleDbRequest(
    private val style: Style,
    private val dependency: Style?,
) : DbRequest {
    override val needsWrite: Boolean
        get() = true

    override fun process(database: Realm) {
        val (rStyle, _) = style(this.style.identifier, database = database)
        rStyle.href = this.style.href
        rStyle.title = this.style.title
        rStyle.updated = this.style.updated
        rStyle.filename = this.style.filename
        rStyle.installed = true
        rStyle.supportsBibliography = this.style.supportsBibliography
        rStyle.isNoteStyle = this.style.isNoteStyle
        rStyle.defaultLocale = this.style.defaultLocale ?: ""

        val dependency = this.dependency

        if (dependency != null) {
            val (rDependency, existed) = style(
                identifier = dependency.identifier,
                database = database
            )
            rDependency.updated = dependency.updated
            rDependency.filename = dependency.filename
            rDependency.href = dependency.href
            rDependency.title = dependency.title
            rDependency.supportsBibliography = dependency.supportsBibliography
            rDependency.isNoteStyle = dependency.isNoteStyle
            rDependency.defaultLocale = dependency.defaultLocale ?: ""
            if (!existed) {
                rDependency.installed = false
            }

            rStyle.supportsBibliography = dependency.supportsBibliography
            rStyle.dependency = rDependency
        }
    }

    private fun style(identifier: String, database: Realm): Pair<RStyle, Boolean> {
        val existing = database.where<RStyle>().equalTo("identifier", identifier).findFirst()
        if (existing != null) {
            return existing to true
        } else {
            val rStyle = database.createObject<RStyle>(identifier)
            return rStyle to false
        }
    }
}
package org.zotero.android.database.requests

import io.realm.Realm
import io.realm.kotlin.where
import org.zotero.android.database.DbError
import org.zotero.android.database.DbResponseRequest
import org.zotero.android.database.objects.RStyle

class ReadStyleDbRequest(private val identifier: String) : DbResponseRequest<RStyle> {
    override val needsWrite: Boolean
        get() = false

    override fun process(database: Realm): RStyle {
        val style = database.where<RStyle>().equalTo("identifier", this.identifier).findFirst() ?: {
            throw DbError.objectNotFound
        }
        return style as RStyle
    }
}